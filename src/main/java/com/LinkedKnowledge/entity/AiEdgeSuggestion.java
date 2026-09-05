package com.LinkedKnowledge.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI 联想边的原始建议记录
 *
 * 与 NodeEdge 的区别:
 *  - NodeEdge 是"实际生效的边",参与反向链接查询
 *  - AiEdgeSuggestion 是"AI 算出来的待审建议",用户拒绝后不删 NodeEdge
 *    (因为 NodeEdge 还没建),只有接受才建 NodeEdge
 */
@Entity
@Table(name = "ai_edge_suggestion", indexes = {
        @Index(name = "idx_user_status", columnList = "user_id,status"),
        @Index(name = "idx_source", columnList = "source_node_id")
})
@Data
public class AiEdgeSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_node_id", nullable = false)
    private Long sourceNodeId;

    @Column(name = "target_node_id", nullable = false)
    private Long targetNodeId;

    /** 0.0 - 1.0,越高越相关 */
    @Column(nullable = false)
    private Double score;

    /** "节点标题相似度 0.78" 这种可读原因 */
    @Column(name = "suggestion_reason", length = 500)
    private String suggestionReason;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AiSuggestionStatus status = AiSuggestionStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;
}
