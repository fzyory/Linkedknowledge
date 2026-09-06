package com.LinkedKnowledge.dto;

import com.LinkedKnowledge.entity.AiSuggestionStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiSuggestionWithTitle {
    private Long id;
    private Long sourceNodeId;
    private String sourceNodeTitle;
    private Long targetNodeId;
    private String targetNodeTitle;
    private Double score;
    private String suggestionReason;
    private AiSuggestionStatus status;
    private LocalDateTime createdAt;
}
