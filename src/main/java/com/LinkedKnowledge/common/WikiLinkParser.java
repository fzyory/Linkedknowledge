package com.LinkedKnowledge.common;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析 [[xxx]] 语法
 *
 * 支持:
 *   [[节点标题]]                → 单链接
 *   [[节点标题|别名]]            → 显示别名,链接到"节点标题"
 *   [[节点标题#块id]]            → 块引用(本期暂存标题,不解析块,放 P6)
 */
public class WikiLinkParser {
    // 匹配 [[xxx]],xxx 里不能有 [] 和换行
    private static final Pattern PATTERN = Pattern.compile("\\[\\[([^\\[\\]\\n]+?)\\]\\]");

    public static List<String> extract(String content) {
        if (content == null || content.isBlank()) return List.of();
        List<String> result = new ArrayList<>();
        Matcher m = PATTERN.matcher(content);
        while (m.find()) {
            String raw = m.group(1).trim();
            // 处理 |别名
            int pipe = raw.indexOf('|');
            if (pipe >= 0) raw = raw.substring(0, pipe).trim();
            // 处理 #块引用(本期先存标题)
            int hash = raw.indexOf('#');
            if (hash >= 0) raw = raw.substring(0, hash).trim();
            if (!raw.isEmpty()) result.add(raw);
        }
        return result;
    }
}
