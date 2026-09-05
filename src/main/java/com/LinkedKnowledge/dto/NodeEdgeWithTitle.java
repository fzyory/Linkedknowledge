package com.LinkedKnowledge.dto;

import com.LinkedKnowledge.entity.EdgeType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NodeEdgeWithTitle {
    private Long edgeId;
    private Long sourceNodeId;
    private String sourceNodeTitle;
    private Long targetNodeId;
    private String targetNodeTitle;
    private EdgeType edgeType;
    private LocalDateTime createdAt;
}
