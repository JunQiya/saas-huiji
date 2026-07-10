package com.huiji.service;

import com.huiji.common.BizException;
import com.huiji.common.ErrorCode;
import com.huiji.common.PageData;
import com.huiji.dto.CouponDto;
import com.huiji.dto.MemberDto;
import com.huiji.entity.CouponRecord;
import com.huiji.entity.Member;
import com.huiji.entity.MemberTag;
import com.huiji.entity.WalletTransaction;
import com.huiji.repository.CouponRecordRepository;
import com.huiji.repository.MemberRepository;
import com.huiji.repository.MemberTagRepository;
import com.huiji.repository.WalletTransactionRepository;
import com.huiji.security.LoginUser;
import com.huiji.security.LoginUserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** 会员服务: 增删改查、储值充值、消费扣款、标签、流水、等级自动升级。 */
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberTagRepository memberTagRepository;
    private final WalletTransactionRepository walletRepository;
    private final CouponRecordRepository couponRecordRepository;
    private final CouponService couponService;
    private final SettingsService settingsService;
    private final AuditHelper auditHelper;

    /** 会员列表(关键字/等级/标签/门店筛选) */
    public PageData<Map<String, Object>> list(String keyword, Integer level, String tag, List<Long> storeIds,
                                              int page, int size) {
        Long tenantId = LoginUserHolder.currentTenantId();
        String storeFilter = (storeIds == null || storeIds.isEmpty()) ? null : String.valueOf(storeIds.get(0));
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size <= 0 ? 20 : size);
        Page<Member> p = memberRepository.search(tenantId, trim(keyword), level, storeFilter, pageable);

        // 标签筛选: 先按标签取会员 id 集合, 内存过滤
        List<Member> members = p.getContent();
        if (tag != null && !tag.isBlank()) {
            Set<Long> tagMemberIds = new HashSet<>(memberTagRepository.findMemberIdsByTag(tenantId, tag));
            members = members.stream().filter(m -> tagMemberIds.contains(m.getId())).collect(Collectors.toList());
        }
        List<Map<String, Object>> list = members.stream().map(m -> toVO(m, true)).collect(Collectors.toList());
        long total = tag == null || tag.isBlank() ? p.getTotalElements() : members.size();
        return PageData.of(list, total, page, size);
    }

    @Transactional
    public Map<String, Object> create(MemberDto.MemberRequest req) {
        Long tenantId = LoginUserHolder.currentTenantId();
        if (memberRepository.existsByPhoneAndTenantIdAndDeletedFalse(req.getPhone(), tenantId)) {
            throw new BizException(ErrorCode.CONFLICT, "手机号已存在");
        }
        Member m = new Member();
        m.setTenantId(tenantId);
        m.setName(req.getName());
        m.setPhone(req.getPhone());
        m.setGender(req.getGender() == null ? "UNKNOWN" : req.getGender());
        m.setBirthday(req.getBirthday());
        m.setStoreIds(req.getStoreIds() == null ? new ArrayList<>() : req.getStoreIds());
        m.setRemark(req.getRemark());
        memberRepository.save(m);
        saveTags(tenantId, m.getId(), req.getTags());
        auditHelper.record("新增会员", "member:" + m.getId(), req.getName() + "/" + req.getPhone());
        return toVO(m, true);
    }

    @Transactional
    public Map<String, Object> update(Long id, MemberDto.MemberRequest req) {
        Long tenantId = LoginUserHolder.currentTenantId();
        Member m = memberRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "会员不存在"));
        if (req.getName() != null) m.setName(req.getName());
        if (req.getPhone() != null) {
            if (!req.getPhone().equals(m.getPhone())
                    && memberRepository.existsByPhoneAndTenantIdAndDeletedFalse(req.getPhone(), tenantId)) {
                throw new BizException(ErrorCode.CONFLICT, "手机号已存在");
            }
            m.setPhone(req.getPhone());
        }
        if (req.getGender() != null) m.setGender(req.getGender());
        if (req.getBirthday() != null) m.setBirthday(req.getBirthday());
        if (req.getStoreIds() != null) m.setStoreIds(req.getStoreIds());
        if (req.getRemark() != null) m.setRemark(req.getRemark());
        memberRepository.save(m);
        if (req.getTags() != null) saveTags(tenantId, id, req.getTags());
        auditHelper.record("编辑会员", "member:" + id, req.getName());
        return toVO(m, true);
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = LoginUserHolder.currentTenantId();
        Member m = memberRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "会员不存在"));
        m.setDeleted(true);
        memberRepository.save(m);
        auditHelper.record("删除会员", "member:" + id, m.getName());
    }

    public Map<String, Object> detail(Long id) {
        Long tenantId = LoginUserHolder.currentTenantId();
        Member m = memberRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "会员不存在"));
        return toVO(m, true);
    }

    /** 会员资金流水 */
    public PageData<Map<String, Object>> transactions(Long memberId, String type, int page, int size) {
        Long tenantId = LoginUserHolder.currentTenantId();
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size <= 0 ? 20 : size);
        Page<WalletTransaction> p = walletRepository.findByMember(tenantId, memberId, trim(type), pageable);
        List<Map<String, Object>> list = p.getContent().stream().map(this::txVO).collect(Collectors.toList());
        return PageData.of(list, p.getTotalElements(), page, size);
    }

    /** 储值充值(含赠送规则: amount 为本次充值, gift 可指定或按规则匹配) */
    @Transactional
    public Map<String, Object> recharge(Long memberId, MemberDto.RechargeRequest req) {
        Long tenantId = LoginUserHolder.currentTenantId();
        Member m = memberRepository.findByIdAndTenantIdAndDeletedFalse(memberId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "会员不存在"));
        if (req.getAmount() == null || req.getAmount() <= 0) {
            throw new BizException(ErrorCode.VALIDATION, "充值金额必须大于 0");
        }
        // 赠送金额: 显式传入优先, 否则按租户储值规则匹配
        long gift = req.getGift() != null ? req.getGift() : settingsService.matchGift(tenantId, req.getAmount());

        long before = m.getBalance() == null ? 0L : m.getBalance();
        long after = before + req.getAmount() + gift;
        m.setBalance(after);
        memberRepository.save(m);

        // 充值流水(本金)
        WalletTransaction tx = new WalletTransaction();
        tx.setTenantId(tenantId);
        tx.setMemberId(memberId);
        tx.setType("RECHARGE");
        tx.setAmount(req.getAmount());
        tx.setGift(gift);
        tx.setBalanceAfter(after);
        tx.setPayMethod(req.getPayMethod() == null ? "CASH" : req.getPayMethod());
        tx.setRemark(req.getRemark());
        tx.setOperatorId(LoginUserHolder.get().getUserId());
        walletRepository.save(tx);

        // 赠送单独记一条 GIFT 流水, 便于统计
        if (gift > 0) {
            WalletTransaction giftTx = new WalletTransaction();
            giftTx.setTenantId(tenantId);
            giftTx.setMemberId(memberId);
            giftTx.setType("GIFT");
            giftTx.setAmount(gift);
            giftTx.setBalanceAfter(after);
            giftTx.setRemark("充值赠送");
            giftTx.setOperatorId(LoginUserHolder.get().getUserId());
            walletRepository.save(giftTx);
        }
        auditHelper.record("会员充值", "member:" + memberId,
                "充" + req.getAmount() + "赠" + gift + ",余额" + after);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("balance", after);
        resp.put("gift", gift);
        return resp;
    }

    /** 消费扣款: 优先扣储值余额, 不足则报错; 若传入 couponCode 则核销对应券 */
    @Transactional
    public Map<String, Object> consume(Long memberId, MemberDto.ConsumeRequest req) {
        Long tenantId = LoginUserHolder.currentTenantId();
        Member m = memberRepository.findByIdAndTenantIdAndDeletedFalse(memberId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "会员不存在"));
        if (req.getAmount() == null || req.getAmount() <= 0) {
            throw new BizException(ErrorCode.VALIDATION, "消费金额必须大于 0");
        }
        long balance = m.getBalance() == null ? 0L : m.getBalance();
        if (balance < req.getAmount()) {
            throw new BizException(ErrorCode.BIZ_ERROR, "储值余额不足, 当前余额 " + balance + " 分");
        }
        // 若使用券, 先核销(失败则整笔消费回滚)
        if (req.getCouponCode() != null && !req.getCouponCode().isBlank()) {
            CouponDto.VerifyRequest vr = new CouponDto.VerifyRequest();
            vr.setCode(req.getCouponCode().trim());
            vr.setStoreId(req.getStoreId());
            couponService.verify(vr);
        }
        long after = balance - req.getAmount();
        m.setBalance(after);
        m.setConsumeCount((m.getConsumeCount() == null ? 0 : m.getConsumeCount()) + 1);
        m.setTotalAmount((m.getTotalAmount() == null ? 0L : m.getTotalAmount()) + req.getAmount());
        m.setLastConsumeAt(LocalDateTime.now());
        // 等级自动升级
        upgradeLevel(tenantId, m);
        memberRepository.save(m);

        WalletTransaction tx = new WalletTransaction();
        tx.setTenantId(tenantId);
        tx.setMemberId(memberId);
        tx.setType("CONSUME");
        tx.setAmount(-req.getAmount());
        tx.setBalanceAfter(after);
        tx.setStoreId(req.getStoreId());
        tx.setOrderNo(genOrderNo());
        // remark 优先记服务项, 便于看板 top-services 聚合
        tx.setRemark((req.getItems() != null && !req.getItems().isBlank()) ? req.getItems() : req.getRemark());
        tx.setOperatorId(LoginUserHolder.get().getUserId());
        walletRepository.save(tx);

        String auditDetail = "扣" + req.getAmount() + ",余额" + after;
        if (req.getCouponCode() != null && !req.getCouponCode().isBlank()) {
            auditDetail += ",券" + req.getCouponCode().trim();
        }
        auditHelper.record("会员消费", "member:" + memberId, auditDetail);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("balance", after);
        resp.put("level", m.getLevel());
        resp.put("levelName", settingsService.levelName(tenantId, m.getLevel()));
        return resp;
    }

    /** 更新会员标签(全量覆盖) */
    @Transactional
    public void updateTags(Long memberId, List<String> tags) {
        Long tenantId = LoginUserHolder.currentTenantId();
        memberRepository.findByIdAndTenantIdAndDeletedFalse(memberId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "会员不存在"));
        saveTags(tenantId, memberId, tags);
        auditHelper.record("更新会员标签", "member:" + memberId, tags == null ? "" : String.join(",", tags));
    }


    /** 批量设置标签(全量覆盖) */
    @Transactional
    public void batchSetTags(List<Long> memberIds, List<String> tags) {
        Long tenantId = LoginUserHolder.currentTenantId();
        if (memberIds == null || memberIds.isEmpty()) return;
        List<Member> members = memberRepository.findAllById(memberIds);
        for (Member m : members) {
            if (!m.getTenantId().equals(tenantId) || Boolean.TRUE.equals(m.getDeleted())) continue;
            saveTags(tenantId, m.getId(), tags);
        }
        auditHelper.record("批量设置标签", "members",
                memberIds.size() + "位,标签:" + (tags == null ? "" : String.join(",", tags)));
    }

    /** 批量调整等级 */
    @Transactional
    public void batchSetLevel(List<Long> memberIds, Integer level) {
        Long tenantId = LoginUserHolder.currentTenantId();
        if (memberIds == null || memberIds.isEmpty() || level == null) return;
        List<Member> members = memberRepository.findAllById(memberIds);
        int updated = 0;
        for (Member m : members) {
            if (!m.getTenantId().equals(tenantId) || Boolean.TRUE.equals(m.getDeleted())) continue;
            m.setLevel(level);
            memberRepository.save(m);
            updated++;
        }
        auditHelper.record("批量调整等级", "members",
                updated + "位 -> " + level);
    }

    /** CSV 导入: 解析 name/phone/gender/birthday, 返回成功/失败 */
    public Map<String, Object> importCsv(java.io.InputStream in) {
        Long tenantId = LoginUserHolder.currentTenantId();
        int success = 0, failed = 0;
        List<String> errors = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            int lineNo = 0;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (first) { first = false; if (line.toLowerCase().contains("name")) continue; }
                if (line.isBlank()) continue;
                String[] cols = line.split(",", -1);
                if (cols.length < 2) {
                    failed++;
                    errors.add("第" + lineNo + "行: 至少需要姓名、手机号两列");
                    continue;
                }
                try {
                    String name = cols[0].trim();
                    String phone = cols[1].trim();
                    if (name.isEmpty() || phone.isEmpty()) {
                        failed++;
                        errors.add("第" + lineNo + "行: 姓名或手机号为空");
                        continue;
                    }
                    if (memberRepository.existsByPhoneAndTenantIdAndDeletedFalse(phone, tenantId)) {
                        failed++;
                        errors.add("第" + lineNo + "行: 手机号已存在");
                        continue;
                    }
                    String gender = cols.length > 2 ? cols[2].trim() : "UNKNOWN";
                    if (gender.isEmpty()) gender = "UNKNOWN";
                    LocalDate birthday = null;
                    if (cols.length > 3 && !cols[3].trim().isEmpty()) {
                        try { birthday = LocalDate.parse(cols[3].trim()); } catch (Exception ignored) {}
                    }
                    Member m = new Member();
                    m.setTenantId(tenantId);
                    m.setName(name);
                    m.setPhone(phone);
                    m.setGender(gender);
                    m.setBirthday(birthday);
                    m.setStoreIds(new ArrayList<>());
                    memberRepository.save(m);
                    success++;
                } catch (Exception ex) {
                    failed++;
                    errors.add("第" + lineNo + "行: " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            throw new BizException(ErrorCode.BIZ_ERROR, "CSV 解析失败: " + e.getMessage());
        }
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", success);
        resp.put("failed", failed);
        resp.put("errors", errors.size() > 50 ? errors.subList(0, 50) : errors);
        auditHelper.record("导入会员", "members", "成功" + success + ",失败" + failed);
        return resp;
    }

    /** CSV 导出: 按当前筛选条件导出 */
    public byte[] exportCsv(String keyword, Integer level, String tag) {
        Long tenantId = LoginUserHolder.currentTenantId();
        // 取所有匹配(不限制分页, 上限 10000 防止 OOM)
        Pageable pageable = PageRequest.of(0, 10000);
        Page<Member> p = memberRepository.search(tenantId, trim(keyword), level, null, pageable);
        List<Member> members = p.getContent();
        if (tag != null && !tag.isBlank()) {
            Set<Long> tagMemberIds = new HashSet<>(memberTagRepository.findMemberIdsByTag(tenantId, tag));
            members = members.stream().filter(m -> tagMemberIds.contains(m.getId())).collect(Collectors.toList());
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\uFEFF"); // BOM, Excel 友好
        sb.append("姓名,手机号,性别,生日,等级,储值余额,累计消费,消费次数,标签\n");
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (Member m : members) {
            List<String> tags = memberTagRepository.findByTenantIdAndMemberId(tenantId, m.getId())
                    .stream().map(MemberTag::getTag).collect(Collectors.toList());
            sb.append(csv(m.getName())).append(",")
              .append(csv(m.getPhone())).append(",")
              .append(csv(m.getGender())).append(",")
              .append(m.getBirthday() == null ? "" : m.getBirthday().format(df)).append(",")
              .append(settingsService.levelName(tenantId, m.getLevel())).append(",")
              .append(m.getBalance() == null ? 0 : m.getBalance()).append(",")
              .append(m.getTotalAmount() == null ? 0 : m.getTotalAmount()).append(",")
              .append(m.getConsumeCount() == null ? 0 : m.getConsumeCount()).append(",")
              .append(csv(String.join("|", tags)))
              .append("\n");
        }
        auditHelper.record("导出会员", "members", members.size() + "位");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String csv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    /** 会员画像: 消费力/活跃度/生命周期/趋势/建议 */
    public Map<String, Object> profile(Long memberId) {
        Long tenantId = LoginUserHolder.currentTenantId();
        Member m = memberRepository.findByIdAndTenantIdAndDeletedFalse(memberId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "会员不存在"));

        // 消费力 = 该会员累计消费在租户内的分位 (0-100)
        List<Member> all = memberRepository.findAllById(memberRepository.allMemberIds(tenantId));
        long myTotal = m.getTotalAmount() == null ? 0L : m.getTotalAmount();
        long consumeScore = computePercentile(myTotal, all.stream().map(x -> x.getTotalAmount() == null ? 0L : x.getTotalAmount()).collect(Collectors.toList()));

        // 活跃度 = 基于最近消费时间, 30 天内 100, 每多 30 天 -25
        long activeScore;
        if (m.getLastConsumeAt() == null) activeScore = 0;
        else {
            long days = java.time.Duration.between(m.getLastConsumeAt(), LocalDateTime.now()).toDays();
            activeScore = Math.max(0, 100 - days / 30 * 25);
        }

        // 生命周期
        String lifecycle;
        if (m.getConsumeCount() == null || m.getConsumeCount() == 0) lifecycle = "新客";
        else if (m.getConsumeCount() <= 3) lifecycle = "成长期";
        else if (activeScore >= 60) lifecycle = "成熟期";
        else if (activeScore >= 25) lifecycle = "衰退期";
        else lifecycle = "流失期";

        // 30 天每日消费
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> trend = new ArrayList<>();
        long sum30 = 0;
        for (int i = 29; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            LocalDateTime ds = d.atStartOfDay();
            LocalDateTime de = d.plusDays(1).atStartOfDay();
            long dayAmount = 0;
            Page<WalletTransaction> txs = walletRepository.searchGlobal(tenantId, "CONSUME", null, memberId, ds, de, null,
                    PageRequest.of(0, 100));
            for (WalletTransaction t : txs.getContent()) dayAmount += Math.abs(t.getAmount());
            sum30 += dayAmount;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", d.toString());
            row.put("amount", dayAmount);
            trend.add(row);
        }

        // 智能建议
        String hint;
        if (activeScore == 0) hint = "客户已沉睡, 建议发 30 元代金券唤醒";
        else if (activeScore < 50) hint = "活跃下降, 建议推送 50 元代金券";
        else if (sum30 > 50000) hint = "近期消费旺盛, 可推送高客单服务";
        else if (consumeScore >= 80) hint = "高价值客户, 建议维护关系, 推送新品体验";
        else hint = "可推送满减券, 提升复购频次";

        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("consumeScore", (int) consumeScore);
        vo.put("activeScore", (int) activeScore);
        vo.put("lifecycle", lifecycle);
        vo.put("trend30d", trend);
        vo.put("tags", memberTagRepository.findByTenantIdAndMemberId(tenantId, m.getId())
                .stream().map(MemberTag::getTag).collect(Collectors.toList()));
        vo.put("nextActionHint", hint);
        return vo;
    }

    /** 分位计算(0-100): 排序后取中位百分位 */
    private long computePercentile(long myValue, List<Long> values) {
        if (values.isEmpty()) return 0;
        List<Long> sorted = values.stream().sorted().collect(Collectors.toList());
        int idx = 0;
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i) >= myValue) { idx = i; break; }
            idx = i;
        }
        return Math.round(idx * 100.0 / Math.max(1, sorted.size() - 1));
    }

    /** 会员持有的券 */
    public List<Map<String, Object>> memberCoupons(Long memberId) {
        Long tenantId = LoginUserHolder.currentTenantId();
        List<CouponRecord> records = couponRecordRepository.findByMember(tenantId, memberId, null);
        return records.stream().map(this::couponRecordVO).collect(Collectors.toList());
    }

    // ---- 内部方法 ----

    /** 等级自动升级: 累计消费达到更高等级阈值则升级, 不降级 */
    private void upgradeLevel(Long tenantId, Member m) {
        var rule = settingsService.resolveLevel(tenantId, m.getTotalAmount());
        if (rule != null && (m.getLevel() == null || rule.getLevel() > m.getLevel())) {
            m.setLevel(rule.getLevel());
        }
    }

    private void saveTags(Long tenantId, Long memberId, List<String> tags) {
        memberTagRepository.deleteByTenantIdAndMemberId(tenantId, memberId);
        if (tags == null) return;
        for (String tag : tags) {
            if (tag == null || tag.isBlank()) continue;
            MemberTag t = new MemberTag();
            t.setTenantId(tenantId);
            t.setMemberId(memberId);
            t.setTag(tag.trim());
            memberTagRepository.save(t);
        }
    }

    public Map<String, Object> toVO(Member m, boolean withTags) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", m.getId());
        vo.put("name", m.getName());
        vo.put("phone", m.getPhone());
        vo.put("gender", m.getGender());
        vo.put("birthday", m.getBirthday());
        vo.put("level", m.getLevel());
        vo.put("levelName", settingsService.levelName(m.getTenantId(), m.getLevel()));
        vo.put("balance", m.getBalance());
        vo.put("points", m.getPoints());
        vo.put("storeIds", m.getStoreIds());
        if (withTags) {
            List<String> tags = memberTagRepository.findByTenantIdAndMemberId(m.getTenantId(), m.getId())
                    .stream().map(MemberTag::getTag).collect(Collectors.toList());
            vo.put("tags", tags);
        }
        vo.put("consumeCount", m.getConsumeCount());
        vo.put("totalAmount", m.getTotalAmount());
        vo.put("lastConsumeAt", m.getLastConsumeAt());
        vo.put("remark", m.getRemark());
        vo.put("createdAt", m.getCreatedAt());
        return vo;
    }

    private Map<String, Object> txVO(WalletTransaction t) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", t.getId());
        vo.put("type", t.getType());
        vo.put("amount", t.getAmount());
        vo.put("gift", t.getGift());
        vo.put("balanceAfter", t.getBalanceAfter());
        vo.put("storeId", t.getStoreId());
        vo.put("payMethod", t.getPayMethod());
        vo.put("orderNo", t.getOrderNo());
        vo.put("remark", t.getRemark());
        vo.put("createdAt", t.getCreatedAt());
        return vo;
    }

    private Map<String, Object> couponRecordVO(CouponRecord r) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", r.getId());
        vo.put("memberId", r.getMemberId());
        vo.put("memberName", r.getMemberName());
        vo.put("couponName", r.getCouponName());
        vo.put("code", r.getCode());
        vo.put("status", r.getStatus());
        vo.put("grantedAt", r.getGrantedAt());
        vo.put("usedAt", r.getUsedAt());
        vo.put("expireAt", r.getExpireAt());
        return vo;
    }

    private String trim(String s) {
        return s == null ? null : s.trim().isEmpty() ? null : s.trim();
    }

    private String genOrderNo() {
        return "OD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    /** 供其它服务复用的会员 VO 转换(不含标签) */
    public Map<String, Object> briefVO(Member m) {
        return toVO(m, false);
    }
}
