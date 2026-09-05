package com.LinkedKnowledge.common;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析 #tag 语法
 *
 * 规则:
 *   #java           → 标签 "java"
 *   #Spring-Boot    → 标签 "spring-boot"
 *   #后端开发        → 标签 "后端开发"
 *
 * 不解析:
 *   #123            (不允许以数字开头)
 *   ##              (空标签)
 *   #tag with space (空格分隔,不解析第二个)
 */
public class TagParser {
    private static final Pattern PATTERN = Pattern.compile("#([a-zA-Z\\u4e00-\\u9fa5][a-zA-Z0-9_\\-\\u4e00-\\u9fa5]{0,49})");

    public static List<String> extract(String content) {
        if (content == null || content.isBlank()) return List.of();
        Set<String> result = new LinkedHashSet<>();
        Matcher m = PATTERN.matcher(content);
        while (m.find()) {
            String tag = m.group(1).toLowerCase();
            result.add(tag);
        }
        return new ArrayList<>(result);
    }
}
