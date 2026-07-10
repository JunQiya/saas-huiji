package com.huiji.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 公共 CSV 工具: 统一格式
 *  - UTF-8 BOM 头, Excel 友好
 *  - CRLF 换行
 *  - 字段含逗号/引号/换行时用双引号包裹, 内部引号双写转义
 *  - 解析时支持引号包裹(简单实现, 适配由本工具生成的 CSV)
 */
public final class CsvHelper {

    public static final String BOM = "\uFEFF";
    public static final String SEP = ",";
    public static final String EOL = "\r\n";

    private CsvHelper() {}

    /** 把一行的字段序列化为 CSV 单元(含必要的引号转义) */
    public static String escape(String s) {
        if (s == null) return "";
        if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    /** 拼接一行, 自动 escape */
    public static String row(Object... cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) sb.append(SEP);
            sb.append(escape(cells[i] == null ? "" : String.valueOf(cells[i])));
        }
        sb.append(EOL);
        return sb.toString();
    }

    /** 构造导出字节流: BOM + 内容 */
    public static byte[] build(String content) {
        return (BOM + content).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 解析 CSV 输入流, 返回行(已去除 BOM, 已处理引号转义)。
     * 首行若匹配表头标记(包含 name/姓名/手机)会被自动跳过。
     */
    public static List<String[]> parse(InputStream in, boolean skipHeader) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(BOM)) line = line.substring(1);
                if (first && skipHeader) {
                    first = false;
                    String lower = line.toLowerCase();
                    if (lower.contains("name") || lower.contains("姓名") || lower.contains("手机") || lower.contains("phone")) {
                        continue;
                    }
                } else {
                    first = false;
                }
                if (line.isBlank()) continue;
                rows.add(parseLine(line));
            }
        }
        return rows;
    }

    /** 解析单行, 支持引号包裹(只解析最常见格式, 不支持跨行字段) */
    public static String[] parseLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuote) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuote = false;
                    }
                } else {
                    cur.append(c);
                }
            } else {
                if (c == ',') {
                    result.add(cur.toString());
                    cur.setLength(0);
                } else if (c == '"') {
                    inQuote = true;
                } else {
                    cur.append(c);
                }
            }
        }
        result.add(cur.toString());
        return result.toArray(new String[0]);
    }

    /**
     * 简单封装批量导入结果: { success, total, successCount, failedCount, errors:[{row,field,message}] }。
     */
    public static Map<String, Object> importResult(int total, int success, int failed, List<String[]> errors) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", failed == 0);
        resp.put("total", total);
        resp.put("successCount", success);
        resp.put("failedCount", failed);
        List<Map<String, Object>> detail = new ArrayList<>();
        int max = Math.min(50, errors.size());
        for (int i = 0; i < max; i++) {
            String[] e = errors.get(i);
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("row", e.length > 0 ? e[0] : "");
            d.put("field", e.length > 1 ? e[1] : "");
            d.put("message", e.length > 2 ? e[2] : "");
            detail.add(d);
        }
        resp.put("errors", detail);
        return resp;
    }

    /** 将 List<String[]> 简化为单字符串 message, 用于某些字段缺失提示 */
    public static String errMsg(String... parts) {
        return String.join(" / ", Arrays.stream(parts).filter(s -> s != null && !s.isBlank()).toArray(String[]::new));
    }
}
