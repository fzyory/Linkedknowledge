// [框架]    本文件定义 LinkedKnowledge 的核心数据实体 KnowledgeNode (知识点节点)。
//             映射数据库表 knowledge_node, 是整个"知识图谱"业务的数据基础 —
//             节点之间通过 parent/children 自引用构成树形结构, 通过 tags 多对多关联打标签。
//             被以下文件依赖:
//               controller/KnowledgeNodeController (CRUD HTTP 入口)
//               service/KnowledgeNodeService / Impl (业务逻辑)
//               repository/KnowledgeNodeRepository (JPA 查询)
//               dto/* 不直接依赖, 但 MindMapNode 节点复用本类的字段风格(id/title/children)。
// [依赖]    jakarta.persistence.* — JPA 标准注解 (本项目用 jakarta.* 而非 javax.*,
//             因为 Spring Boot 3.x 迁移到了 Jakarta EE 命名空间, 跟 Spring Boot 2.x 不兼容)。
//             lombok.* — Lombok 注解处理器, 自动生成 getter/setter/构造器等样板代码。
//             org.springframework.format.annotation.DateTimeFormat — Spring MVC 日期格式化注解。
//             关联实体: NodeType (枚举: CONCEPT/EXAMPLE/EXERCISE/REFERENCE),
//                       NodeStatus (枚举: DRAFT/PUBLISHED/ARCHIVED),
//                       Tag (多对多关联)。
// [框架妙用] (1) @Entity + @Table — JPA 标准映射, 类名 → 表名 knowledge_node。
//             (2) @Data + @NoArgsConstructor + @AllArgsConstructor — Lombok 三件套,
//              @Data 自动生成 getter/setter/toString/equals/hashCode, 无需手写。
//             (3) @ManyToOne(fetch = LAZY) — 懒加载 parent, 不查 children 就不发 SQL,
//              避免 N+1 查询问题; 实际取 parent 时才触发 SELECT。
//             (4) @OneToMany(mappedBy = "parent", cascade = ALL) —
//              mappedBy 指向 parent 字段, 表示这是双向关联的反向端(本类不维护外键);
//              cascade = ALL 表示 save/delete 本节点时自动级联 children。
//             (5) @ManyToMany + @JoinTable — 节点和标签的多对多, 中间表 node_tag
//              (node_id, tag_id) 自动生成。
//             (6) @PreUpdate — JPA 回调钩子, UPDATE SQL 发出前自动调用, 本类用它刷新 updatedAt。

package com.LinkedKnowledge.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
// Lombok注解
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;
// 时间类
import java.time.LocalDateTime;
// 集合类
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// [框架]    KnowledgeNode 是 LinkedKnowledge 项目的主实体, 一条记录对应"一个知识点节点"。
//             节点可以嵌套(parent/children 自引用树形), 也可以打多个标签(tags 多对多)。
//             典型生命周期: 创建 (DRAFT) → 编辑 → 发布 (PUBLISHED) → 归档 (ARCHIVED)。
//             [数据流]  写入: Controller → Service → Repository.save() → JPA 自动生成 INSERT。
//                       读取: Controller → Service → Repository.findXxx() → JPA 自动生成 SELECT。
//                       删除: Service.deleteNode() → Repository.deleteById() → DELETE, 级联删除 children。
@Entity
@Table(name = "knowledge_node")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@EqualsAndHashCode(exclude = {"parent", "children", "tags"})
@ToString(exclude = {"parent", "children", "tags"})
public class KnowledgeNode {
    // [参数]    主键, MySQL AUTO_INCREMENT 自增。GenerationType.IDENTITY 让数据库自己生成。
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // [参数]    节点标题, 必填, 长度上限 200。用户在 UI 上看到的"知识点名"。
    @Column(nullable = false, length = 200)
    private String title;

    @Column(name="user_id",nullable = false)
    private Long userId;

    // [参数]    节点正文, 可空, MySQL TEXT 类型(可存 64KB)。支持 Markdown / 富文本。
    @Column(columnDefinition = "TEXT")
    private String content;

    /** Obsidian folder path, e.g. "Philosophy/Books" */
    @Column(length = 500)
    private String folderPath = "";

    /** comma-separated aliases, like YAML aliases */
    @Column(length = 1000)
    private String aliases = "";

    /** extra YAML properties as JSON object */
    @Column(columnDefinition = "TEXT")
    private String propertiesJson = "";

    private Boolean starred = false;

    // [参数]    节点类型枚举, 存数据库时存字符串(不是数字下标, 避免枚举顺序变动破坏数据)。
    //             CONCEPT = 概念, EXAMPLE = 示例, EXERCISE = 练习, REFERENCE = 参考资料。
    @Enumerated(EnumType.STRING)
    private NodeType type;  // 枚举：CONCEPT, EXAMPLE, EXERCISE, REFERENCE

    // [参数]    节点状态, 默认 DRAFT (草稿)。@Enumerated(STRING) 让数据库存字符串而非下标。
    //             DRAFT / PUBLISHED / ARCHIVED 三态。
    @Enumerated(EnumType.STRING)
    private NodeStatus status = NodeStatus.DRAFT;

    // [参数]    难度等级 1-5, 默认 1 (最简单)。用于前端做颜色/排序区分。
    private Integer difficultyLevel = 1;  // 1-5

    // [实现]    自引用多对一: 一个节点的父节点也是 KnowledgeNode, parent_id 外键。
    //             fetch = LAZY: 不查 children 时不主动 SELECT parent 表, 避免不必要的 JOIN。
    //             如果节点是顶级节点 (root), parent 为 null。
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonIgnore
    private KnowledgeNode parent;

    // [实现]    自引用一对多: 一个节点的子节点列表。mappedBy = "parent" 表示反向端
    //             (本类不维护外键, 由 parent 字段的 @ManyToOne 维护)。
    //             cascade = ALL: save 本节点时级联 save children, delete 时级联 delete children。
    //             默认初始化为空 ArrayList, 避免 NullPointerException。
    //             @JsonIgnore: open-in-view=false 时懒加载集合不能在序列化时访问。
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<KnowledgeNode> children = new  ArrayList<>();

    // [实现]    多对多: 一个节点可以有多个 Tag, 一个 Tag 也能挂在多个节点上。
    //             中间表 node_tag (node_id, tag_id) 由 JPA 自动生成, 无需手写。
    //             Set 去重: 同一节点不会重复挂同一个 tag。
    @ManyToMany
    @JoinTable(
            name = "node_tag",
            joinColumns = @JoinColumn(name = "node_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    // [实现]    创建时间, 字段初始化为当前时间, JPA INSERT 时自动写入数据库。
    //             updatable = false 让 JPA 后续 UPDATE 不修改这个字段(创建时间不应被改)。
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // [实现]    更新时间, 字段初始化为当前时间, @PreUpdate 回调会自动刷新。
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // [实现]    JPA 回调钩子: 任何 UPDATE SQL 发出前自动调用本方法, 用于自动刷新 updatedAt。
    //             不需要手动在 Service 层 setUpdatedAt(...), JPA 会自动处理。
    //             [注意]  必须是 void 无参方法, protected/private 都行, 不能抛 checked exception。
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}