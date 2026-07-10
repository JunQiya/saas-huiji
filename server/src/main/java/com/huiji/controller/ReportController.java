package com.huiji.controller;

import com.huiji.common.PageData;
import com.huiji.common.Result;
import com.huiji.dto.ReportDto;
import com.huiji.service.ReportService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** 报表订阅接口 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    public Result<PageData<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(reportService.list(page, size));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.success(reportService.detail(id));
    }

    @PostMapping
    public Result<Map<String, Object>> create(@Valid @RequestBody ReportDto.ReportRequest req) {
        return Result.success(reportService.create(req));
    }

    @PutMapping("/{id}")
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody ReportDto.ReportRequest req) {
        return Result.success(reportService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        reportService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/toggle")
    public Result<Map<String, Object>> toggle(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Boolean enabled = body == null ? Boolean.FALSE : Boolean.valueOf(String.valueOf(body.get("enabled")));
        return Result.success(reportService.toggle(id, enabled));
    }

    @PostMapping("/{id}/run")
    public Result<Map<String, Object>> runNow(@PathVariable Long id) {
        return Result.success(reportService.runNow(id));
    }

    @GetMapping("/{id}/download")
    public void download(@PathVariable Long id,
                         @RequestParam(defaultValue = "pdf") String type,
                         HttpServletResponse response) throws Exception {
        File f = reportService.download(id, type);
        if (f == null) {
            response.setStatus(404);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("未找到可下载的报表文件, 请先执行一次");
            return;
        }
        String fileName = URLEncoder.encode(f.getName(), StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setContentLengthLong(f.length());
        try (FileInputStream fis = new FileInputStream(f);
             OutputStream os = response.getOutputStream()) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = fis.read(buf)) > 0) os.write(buf, 0, len);
            os.flush();
        }
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.success(reportService.stats());
    }
}
