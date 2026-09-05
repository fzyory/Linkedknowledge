package com.LinkedKnowledge.dto;

import lombok.Data;

import java.util.List;

@Data
public class GraphData {
    private List<Node> nodes;
    private List<Edge> edges;

    @Data
    public static class Node {
        private Long id;
        private String title;
        private String type;
    }

    @Data
    public static class Edge {
        private Long source;
        private Long target;
        private String type;
    }
}
