package com.huiji.service;

import com.huiji.common.BizException;
import com.huiji.common.ErrorCode;
import com.huiji.common.PageData;
import com.huiji.dto.ReportDto;
import com.huiji.entity.Member;
import com.huiji.entity.ReportTask;
import com.huiji.entity.WalletTransaction;
import com.huiji.repository.MemberRepository;
import com.huiji.repository.ReportTaskRepository;
import com.huiji.repository.WalletTransactionRepository;
import com.huiji.security.LoginUserHolder;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 报表订阅服务:
 *  - CRUD
 *  - runNow: 聚合数据 + 生成 PDF/Excel 临时文件 + 写审计(模拟邮件发送)
 *  - download: 文件流
 *  - 调度: 每天 02:00 扫到期任务(由 ReportSchedulerHook 触发)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final String REPORT_DIR = "/tmp/huiji-reports";
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ReportTaskRepository reportTaskRepository;
    private final MemberRepository memberRepository;
    private final WalletTransactionRepository walletRepository;
    private final AuditHelper auditHelper;

    // ---- CRUD ----

    public PageData<Map<String, Object>> list(int page, int size) {
        Long tenantId = LoginUserHolder.currentTenantId();
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size <= 0 ? 20 : size);
        Page<ReportTask> p = reportTaskRepository.listByTenant(tenantId, pageable);
        List<Map<String, Object>> list = p.getContent().stream().map(this::toVO).toList();
        return PageData.of(list, p.getTotalElements(), page, size);
    }

    public Map<String, Object> detail(Long id) {
        Long tenantId = LoginUserHolder.currentTenantId();
        ReportTask t = reportTaskRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "报表任务不存在"));
        return toVO(t);
    }

    @Transactional
    public Map<String, Object> create(ReportDto.ReportRequest req) {
        Long tenantId = LoginUserHolder.currentTenantId();
        ReportTask t = new ReportTask();
        t.setTenantId(tenantId);
        t.setName(req.getName());
        t.setType(req.getType());
        t.setSchedule(req.getSchedule());
        t.setRecipients(joinEmails(req.getRecipients()));
        t.setEnabled(true);
        t.setNextRunAt(computeNextRun(req.getSchedule(), LocalDateTime.now()));
        reportTaskRepository.save(t);
        auditHelper.record("新建报表任务", "report:" + t.getId(), t.getName());
        return toVO(t);
    }

    @Transactional
    public Map<String, Object> update(Long id, ReportDto.ReportRequest req) {
        Long tenantId = LoginUserHolder.currentTenantId();
        ReportTask t = reportTaskRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "报表任务不存在"));
        if (req.getName() != null) t.setName(req.getName());
        if (req.getType() != null) t.setType(req.getType());
        if (req.getSchedule() != null) {
            t.setSchedule(req.getSchedule());
            t.setNextRunAt(computeNextRun(req.getSchedule(), LocalDateTime.now()));
        }
        if (req.getRecipients() != null) t.setRecipients(joinEmails(req.getRecipients()));
        reportTaskRepository.save(t);
        auditHelper.record("编辑报表任务", "report:" + id, t.getName());
        return toVO(t);
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = LoginUserHolder.currentTenantId();
        ReportTask t = reportTaskRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "报表任务不存在"));
        t.setDeleted(true);
        reportTaskRepository.save(t);
        auditHelper.record("删除报表任务", "report:" + id, t.getName());
    }

    @Transactional
    public Map<String, Object> toggle(Long id, Boolean enabled) {
        Long tenantId = LoginUserHolder.currentTenantId();
        ReportTask t = reportTaskRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "报表任务不存在"));
        t.setEnabled(enabled);
        t.setNextRunAt(enabled ? computeNextRun(t.getSchedule(), LocalDateTime.now()) : null);
        reportTaskRepository.save(t);
        auditHelper.record("报表启停", "report:" + id, enabled ? "启用" : "停用");
        return toVO(t);
    }

    // ---- 生成与下载 ----

    /**
     * 立即运行: 聚合数据 -> 生成 PDF + Excel -> 写审计(模拟邮件发送) -> 更新 lastRunAt
     */
    @Transactional
    public Map<String, Object> runNow(Long id) {
        Long tenantId = LoginUserHolder.currentTenantId();
        ReportTask t = reportTaskRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "报表任务不存在"));

        Map<String, Object> data = aggregateData(tenantId, t.getType());
        LocalDateTime now = LocalDateTime.now();
        String ts = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String base = id + "-" + ts;

        File pdf = null;
        File xlsx = null;
        try {
            File dir = new File(REPORT_DIR + "/" + tenantId);
            if (!dir.exists()) dir.mkdirs();
            pdf = new File(dir, base + ".pdf");
            xlsx = new File(dir, base + ".xlsx");
            writePdf(pdf, t, data);
            writeExcel(xlsx, t, data);
        } catch (Exception e) {
            log.error("报表生成失败 id={}", id, e);
            throw new BizException(ErrorCode.SERVER_ERROR, "报表生成失败: " + e.getMessage());
        }

        t.setLastRunAt(now);
        t.setNextRunAt(computeNextRun(t.getSchedule(), now));
        reportTaskRepository.save(t);

        // 模拟邮件发送 -> 写审计
        auditHelper.record("REPORT_SENT", "report:" + id,
                "recipients=" + (t.getRecipients() == null ? "" : t.getRecipients())
                        + ", pdf=" + (pdf == null ? "" : pdf.getName())
                        + ", xlsx=" + (xlsx == null ? "" : xlsx.getName()));

        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("lastRunAt", t.getLastRunAt());
        vo.put("nextRunAt", t.getNextRunAt());
        vo.put("pdfPath", pdf == null ? null : pdf.getAbsolutePath());
        vo.put("xlsxPath", xlsx == null ? null : xlsx.getAbsolutePath());
        return vo;
    }

    /**
     * 下载文件: pdf/xlsx
     */
    public File download(Long id, String type) {
        Long tenantId = LoginUserHolder.currentTenantId();
        ReportTask t = reportTaskRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "报表任务不存在"));
        if (t.getLastRunAt() == null) {
            // 没有历史生成, 跑一次
            runNow(id);
            t = reportTaskRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId).orElse(t);
        }
        File dir = new File(REPORT_DIR + "/" + tenantId);
        if (!dir.exists()) return null;
        File[] list = dir.listFiles((d, name) -> name.startsWith(id + "-"));
        if (list == null) return null;
        // 选最近文件
        File latest = null;
        long latestMod = 0L;
        String ext = "pdf".equalsIgnoreCase(type) ? ".pdf" : ".xlsx";
        for (File f : list) {
            if (!f.getName().endsWith(ext)) continue;
            if (f.lastModified() > latestMod) {
                latestMod = f.lastModified();
                latest = f;
            }
        }
        return latest;
    }

    /** 获取最新一次生成的下载文件名 + 类型, 找不到时返回 null */
    public Map<String, Object> getLatestGenerated(Long id, String type) {
        File f = download(id, type);
        Map<String, Object> vo = new LinkedHashMap<>();
        if (f == null) return null;
        vo.put("name", f.getName());
        vo.put("size", f.length());
        vo.put("lastModified", f.lastModified());
        return vo;
    }

    /** 统计 */
    public Map<String, Object> stats() {
        Long tenantId = LoginUserHolder.currentTenantId();
        long total = reportTaskRepository.countByTenant(tenantId);
        long enabled = reportTaskRepository.countEnabledByTenant(tenantId);
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        long todayRuns = reportTaskRepository.countRunBetween(tenantId, todayStart, tomorrowStart);
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("todayRuns", todayRuns);
        vo.put("todayFiles", todayRuns * 2); // pdf + xlsx
        vo.put("tasksTotal", total);
        vo.put("tasksEnabled", enabled);
        return vo;
    }

    // ---- 调度: 由 ReportSchedulerHook 每天 02:00 触发 ----

    public void runDue() {
        List<ReportTask> due = reportTaskRepository.findDue(LocalDateTime.now());
        for (ReportTask t : due) {
            try {
                runNow(t.getId());
            } catch (Exception e) {
                log.error("报表定时执行失败 id={}", t.getId(), e);
            }
        }
    }

    // ---- 内部 ----

    /** 聚合数据: 按报表类型生成明细与汇总 */
    private Map<String, Object> aggregateData(Long tenantId, String type) {
        Map<String, Object> data = new LinkedHashMap<>();
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(30);
        List<WalletTransaction> txs = walletRepository.consumeInRange(tenantId, start, end, null);

        // 公共汇总
        long totalAmount = txs.stream().mapToLong(t -> Math.abs(t.getAmount() == null ? 0L : t.getAmount())).sum();
        int orderCount = txs.size();
        List<Member> allMembers = memberRepository.findAllById(memberRepository.allMemberIds(tenantId));

        data.put("reportType", type);
        data.put("rangeStart", start.format(DF));
        data.put("rangeEnd", end.format(DF));
        data.put("totalAmount", totalAmount);
        data.put("orderCount", orderCount);
        data.put("memberCount", allMembers.size());
        data.put("newMembers", allMembers.stream()
                .filter(m -> m.getCreatedAt() != null && m.getCreatedAt().isAfter(start)).count());

        if ("REVENUE".equals(type) || "DASHBOARD".equals(type)) {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (WalletTransaction tx : txs) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("orderNo", tx.getOrderNo());
                r.put("memberId", tx.getMemberId());
                r.put("amount", tx.getAmount());
                r.put("createdAt", tx.getCreatedAt() == null ? "" : tx.getCreatedAt().format(DF));
                r.put("remark", tx.getRemark());
                rows.add(r);
            }
            data.put("rows", rows);
        } else if ("MEMBER".equals(type)) {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Member m : allMembers) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("id", m.getId());
                r.put("name", m.getName());
                r.put("phone", m.getPhone());
                r.put("level", m.getLevel());
                r.put("balance", m.getBalance());
                r.put("totalAmount", m.getTotalAmount());
                r.put("consumeCount", m.getConsumeCount());
                r.put("createdAt", m.getCreatedAt() == null ? "" : m.getCreatedAt().toString());
                rows.add(r);
            }
            data.put("rows", rows);
        } else if ("COUPON".equals(type)) {
            data.put("rows", new ArrayList<>());
        } else if ("ORDER".equals(type)) {
            data.put("rows", new ArrayList<>());
        } else {
            data.put("rows", new ArrayList<>());
        }
        return data;
    }

    /** 生成 PDF: 标题 + 汇总 + 表格 */
    private void writePdf(File out, ReportTask t, Map<String, Object> data) throws Exception {
        Document doc = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(doc, new FileOutputStream(out));
            doc.open();
            Font title = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Font h2 = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD);
            Font body = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
            doc.add(new Paragraph(typeText(t.getType()) + " - " + t.getName(), title));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Range: " + data.get("rangeStart") + " - " + data.get("rangeEnd"), body));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Summary", h2));
            doc.add(new Paragraph("Total Amount (fen): " + data.get("totalAmount"), body));
            doc.add(new Paragraph("Order Count: " + data.get("orderCount"), body));
            doc.add(new Paragraph("Member Count: " + data.get("memberCount"), body));
            doc.add(new Paragraph("New Members: " + data.get("newMembers"), body));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Detail", h2));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rows = (List<Map<String, Object>>) data.get("rows");
            if (rows != null && !rows.isEmpty()) {
                Map<String, Object> first = rows.get(0);
                PdfPTable table = new PdfPTable(first.size());
                table.setWidthPercentage(100);
                for (String key : first.keySet()) {
                    PdfPCell cell = new PdfPCell(new Phrase(key, body));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    table.addCell(cell);
                }
                int max = Math.min(rows.size(), 500);
                for (int i = 0; i < max; i++) {
                    Map<String, Object> r = rows.get(i);
                    for (Object v : r.values()) {
                        table.addCell(new Phrase(v == null ? "" : String.valueOf(v), body));
                    }
                }
                doc.add(table);
                if (rows.size() > max) {
                    doc.add(new Paragraph("(More rows truncated, see Excel for full data)", body));
                }
            } else {
                doc.add(new Paragraph("(No detail rows in this range)", body));
            }
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Generated at " + LocalDateTime.now().format(DF) + " by 星河·会记", body));
        } finally {
            doc.close();
        }
    }

    /** 生成 Excel: 多 sheet (Summary + Detail) */
    private void writeExcel(File out, ReportTask t, Map<String, Object> data) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet summary = wb.createSheet("Summary");
            int r = 0;
            summary.createRow(r++).createCell(0).setCellValue(typeText(t.getType()) + " - " + t.getName());
            summary.createRow(r++).createCell(0).setCellValue("Range: " + data.get("rangeStart") + " - " + data.get("rangeEnd"));
            summary.createRow(r++).createCell(0).setCellValue("Total Amount (fen): " + data.get("totalAmount"));
            summary.createRow(r++).createCell(0).setCellValue("Order Count: " + data.get("orderCount"));
            summary.createRow(r++).createCell(0).setCellValue("Member Count: " + data.get("memberCount"));
            summary.createRow(r++).createCell(0).setCellValue("New Members: " + data.get("newMembers"));
            summary.createRow(r++).createCell(0).setCellValue("Generated at " + LocalDateTime.now().format(DF));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rows = (List<Map<String, Object>>) data.get("rows");
            if (rows != null && !rows.isEmpty()) {
                XSSFSheet detail = wb.createSheet("Detail");
                Row header = detail.createRow(0);
                Map<String, Object> first = rows.get(0);
                int col = 0;
                for (String key : first.keySet()) {
                    header.createCell(col++).setCellValue(key);
                }
                int max = Math.min(rows.size(), 5000);
                for (int i = 0; i < max; i++) {
                    Row row = detail.createRow(i + 1);
                    col = 0;
                    for (Object v : rows.get(i).values()) {
                        Cell c = row.createCell(col++);
                        if (v == null) { c.setCellValue(""); }
                        else if (v instanceof Number) c.setCellValue(((Number) v).doubleValue());
                        else c.setCellValue(String.valueOf(v));
                    }
                }
            }
            try (FileOutputStream fos = new FileOutputStream(out)) {
                wb.write(fos);
            }
        }
    }

    private String typeText(String type) {
        return switch (type == null ? "" : type) {
            case "REVENUE" -> "营业额报表";
            case "MEMBER" -> "会员报表";
            case "COUPON" -> "优惠券报表";
            case "ORDER" -> "订单报表";
            default -> "经营看板报表";
        };
    }

    private String joinEmails(List<String> emails) {
        if (emails == null || emails.isEmpty()) return null;
        return String.join(",", emails.stream()
                .map(s -> s == null ? "" : s.trim())
                .filter(s -> !s.isEmpty())
                .toList());
    }

    private List<String> splitEmails(String s) {
        if (s == null || s.isBlank()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(s.split(",")));
    }

    private LocalDateTime computeNextRun(String schedule, LocalDateTime from) {
        if (schedule == null) return null;
        return switch (schedule) {
            case "DAILY" -> from.plusDays(1);
            case "WEEKLY" -> from.plusWeeks(1);
            case "MONTHLY" -> from.plusMonths(1);
            case "ONCE" -> null;
            default -> from.plusDays(1);
        };
    }

    public Map<String, Object> toVO(ReportTask t) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", t.getId());
        vo.put("name", t.getName());
        vo.put("type", t.getType());
        vo.put("schedule", t.getSchedule());
        vo.put("recipients", splitEmails(t.getRecipients()));
        vo.put("lastRunAt", t.getLastRunAt());
        vo.put("nextRunAt", t.getNextRunAt());
        vo.put("enabled", t.getEnabled());
        vo.put("createdAt", t.getCreatedAt());
        return vo;
    }
}
