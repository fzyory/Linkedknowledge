package com.LinkedKnowledge.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 节点之间的"边" — 支持手动建边 + AI 建议状态机
 *
 * 重要:
 *  - edge_type: 这条边的"出身"(manual_button / parsed_link / ai_suggested),永不修改
 *  - edge_status: 这条边"当前活不活"(pending / accepted / rejected)
 *  - 二者必须分离,不能用 is_ai_suggested 一个布尔代替
 */
@Entity
@Table(name = "node_edge", indexes = {
        @Index(name = "idx_source", columnList = "source_node_id"),
        @Index(name = "idx_target", columnList = "target_node_id"),
        @Index(name = "idx_status_type", columnList = "edge_status,edge_type")
})
@Data
public class NodeEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_node_id", nullable = false)
    private Long sourceNodeId;

    @Column(name = "target_node_id", nullable = false)
    private Long targetNodeId;

    /** 边来源 — 永不修改 */
    @Enumerated(EnumType.STRING)
    @Column(name = "edge_type", nullable = false, length = 32)
    private EdgeType edgeType;

    /** 边的当前状态 */
    @Enumerated(EnumType.STRING)
    @Column(name = "edge_status", nullable = false, length = 32)
    private EdgeStatus edgeStatus = EdgeStatus.ACCEPTED;

    /** 原文(对 parsed_link 有意义) */
    @Column(name = "source_text", length = 500)
    private String sourceText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;
}
