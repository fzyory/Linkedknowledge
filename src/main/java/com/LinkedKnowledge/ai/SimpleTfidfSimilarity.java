package com.LinkedKnowledge.ai;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 简易 TF-IDF 余弦相似度(本期不上 LLM)
 *
 * 步骤:
 *   1. 分词(简单按空格 + 中文按字)
 *   2. 计算每个节点的 TF-IDF 向量
 *   3. 两两算余弦相似度
 *   4. 返回 Top N
 *
 * 性能:1000 节点规模跑一次约 200ms,够用
 */
public class SimpleTfidfSimilarity {

    /** 中文按字切,英文按词切,统一小写 */
    private static List<String> tokenize(String text) {
        if (text == null) return List.of();
        String s = text.toLowerCase();
        List<String> tokens = new ArrayList<>();
        // 英文/数字 连续段
        StringBuilder en = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                en.append(c);
            } else {
                if (en.length() > 0) { tokens.add(en.toString()); en.setLength(0); }
                // 中文单字
                if (c >= 0x4E00 && c <= 0x9FA5) tokens.add(String.valueOf(c));
            }
        }
        if (en.length() > 0) tokens.add(en.toString());
        return tokens;
    }

    private static Map<String, Double> tfidf(List<String> tokens, Map<String, Integer> df) {
        if (tokens.isEmpty()) return Map.of();
        Map<String, Integer> tf = new HashMap<>();
        for (String t : tokens) tf.merge(t, 1, Integer::sum);
        int total = tokens.size();
        Map<String, Double> vec = new HashMap<>();
        for (var e : tf.entrySet()) {
            double idf = Math.log((double) (df.size() + 1) / (df.getOrDefault(e.getKey(), 0) + 1)) + 1;
            vec.put(e.getKey(), (double) e.getValue() / total * idf);
        }
        return vec;
    }

    private static double cosine(Map<String, Double> a, Map<String, Double> b) {
        double dot = 0, na = 0, nb = 0;
        for (var e : a.entrySet()) {
            na += e.getValue() * e.getValue();
            if (b.containsKey(e.getKey())) dot += e.getValue() * b.get(e.getKey());
        }
        for (var v : b.values()) nb += v * v;
        if (na == 0 || nb == 0) return 0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    /**
     * @param docs Map<docId, text> 待比较的所有文档
     * @param queryId 要找"和它相关"的文档 id
     * @param topN 取前 N 个
     * @return List of (docId, score, reason)
     */
    public static List<Suggestion> findTopSimilar(Map<Long, String> docs, Long queryId, int topN) {
        if (!docs.containsKey(queryId) || docs.size() < 2) return List.of();
        String queryText = docs.get(queryId);
        if (queryText == null || queryText.isBlank()) return List.of();

        // 1. 算所有 doc 的 TF
        Map<Long, List<String>> allTokens = new HashMap<>();
        for (var e : docs.entrySet()) allTokens.put(e.getKey(), tokenize(e.getValue()));

        // 2. 算 DF
        Map<String, Integer> df = new HashMap<>();
        for (var tokens : allTokens.values()) {
            Set<String> uniq = new HashSet<>(tokens);
            for (String t : uniq) df.merge(t, 1, Integer::sum);
        }

        // 3. 算 query 向量
        Map<String, Double> queryVec = tfidf(allTokens.get(queryId), df);

        // 4. 和其他所有 doc 算相似度
        List<Suggestion> results = new ArrayList<>();
        for (var e : allTokens.entrySet()) {
            if (e.getKey().equals(queryId)) continue;
            Map<String, Double> otherVec = tfidf(e.getValue(), df);
            double sim = cosine(queryVec, otherVec);
            if (sim > 0.3) {
                results.add(new Suggestion(e.getKey(), sim, "标题/内容相似度 " + String.format("%.2f", sim)));
            }
        }

        // 5. 取 Top N
        return results.stream()
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(topN)
                .collect(Collectors.toList());
    }

    public record Suggestion(Long docId, double score, String reason) {}
}
