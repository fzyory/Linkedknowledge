package com.LinkedKnowledge.entity;
import jakarta.persistence.*;
// Lombok注解
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
// 时间类
import java.time.LocalDateTime;
// 集合类
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
@Entity
@Table(name = "knowledge_node")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeNode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private NodeType type;  // 枚举：CONCEPT, EXAMPLE, EXERCISE, REFERENCE

    @Enumerated(EnumType.STRING)
    private NodeStatus status = NodeStatus.DRAFT;

    private Integer difficultyLevel = 1;  // 1-5

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private KnowledgeNode parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<KnowledgeNode> children = new  ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "node_tag",
            joinColumns = @JoinColumn(name = "node_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}