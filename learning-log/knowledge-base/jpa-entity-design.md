# JPA 实体设计知识

记录 JPA 实体类设计相关的知识点。

---

## 基本注解

### @Entity 和 @Table
```java
@Entity
@Table(name = "knowledge_node")
public class KnowledgeNode {
    // ...
}
```

**作用**：
- `@Entity` 标记这是一个 JPA 实体
- `@Table` 指定数据库表名

### @Id 和 @GeneratedValue
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

**作用**：
- `@Id` 标记主键
- `@GeneratedValue` 自动生成主键值
- `IDENTITY` 策略使用数据库自增

---

## 关系映射

### @ManyToOne（多对一）
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "parent_id")
private KnowledgeNode parent;
```

### @OneToMany（一对多）
```java
@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
private List<KnowledgeNode> children = new ArrayList<>();
```

### @ManyToMany（多对多）
```java
@ManyToMany
@JoinTable(
    name = "node_tag",
    joinColumns = @JoinColumn(name = "node_id"),
    inverseJoinColumns = @JoinColumn(name = "tag_id")
)
private Set<Tag> tags = new HashSet<>();
```

**知识点**：
- `FetchType.LAZY` 延迟加载，提高性能
- `cascade` 级联操作
- `mappedBy` 指定关系的维护方

---
