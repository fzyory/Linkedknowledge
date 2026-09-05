package com.LinkedKnowledge.dto;

import com.LinkedKnowledge.entity.EdgeType;
import lombok.Data;

@Data
public class EdgeCreateRequest {
    private Long sourceNodeId;
    private Long targetNodeId;
    private EdgeType edgeType;       // MANUAL_BUTTON / PARSED_LINK / AI_SUGGESTED
    private String sourceText;        // 对 PARSED_LINK 有意义
    private Long suggestionId;        // 对 AI_SUGGESTED 有意义(关联 AiEdgeSuggestion.id)
}
