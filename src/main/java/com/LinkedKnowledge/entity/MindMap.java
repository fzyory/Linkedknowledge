package com.LinkedKnowledge.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 思维导图实体 —— 存用户生成的导图
 *
 * 简化版设计：导图用 JSON 字符串存（TEXT 字段）
 * 适合小到中等规模导图（< 1MB JSON）
 * 大规模导图建议拆 node 表（后续扩展）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mind_map")
public class MindMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 导图标题（一般就是 topic） */
    @Column(nullable = false, length = 200)
    private String title;

    /** 用户输入的主题 */
    @Column(nullable = false, length = 500)
    private String topic;

    /** 用户输入的上下文（可选） */
    @Column(columnDefinition = "TEXT")
    private String context;

    /** 导图树形结构 JSON */
    @Lob
    @Column(name = "tree_json", columnDefinition = "LONGTEXT", nullable = false)
    private String treeJson;

    /** 所属用户 ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}