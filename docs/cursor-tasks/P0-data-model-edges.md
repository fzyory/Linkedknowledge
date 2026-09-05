# P0 — 数据模型重构:边表 + AI 建议表

## 1. 目标

把现有的 mindmap 数据模型从「节点父子树」重构为「节点 + 显式边表 + AI 建议状态机」。这是后面 P1~P4 的**基础**,必须先做完。

## 2. 验收标准(可勾选)

- [ ] `NodeEdge` 实体类创建,对应表 `node_edge`
- [ ] `AiEdgeSuggestion` 实体类创建,对应表 `ai_edge_suggestion`
- [ ] `NodeEdge` 字段完整:`id`, `source_node_id`, `target_node_id`, `edge_type`, `edge_status`, `source_text`, `created_at`, `accepted_at`, `rejected_at`
- [ ] `AiEdgeSuggestion` 字段完整:`id`, `source_node_id`, `target_node_id`, `suggestion_reason`, `score`, `status`, `created_at`, `decided_at`, `user_id`
- [ ] `KnowledgeNode` 加 `user_id` 字段(已有,确认存在)
- [ ] `application.yml` 确认 `ddl-auto=update`,启动后能看到 `node_edge` 和 `ai_edge_suggestion` 两张新表自动建好
- [ ] 老的 `knowledge_node.parent_id / children` 字段**保留**(本期不删,因为 children 列表还在用,但不再用于"边")
- [ ] `NodeEdgeRepository`, `AiEdgeSuggestionRepository` 创建,带基础查询方法
- [ ] `POST /api/edges` `DELETE /api/edges/{id}` `GET /api/edges/source/{nodeId}` `GET /api/edges/target/{nodeId}` 四个接口实现

## 3. 当前代码现状(供 Cursor 定位)

- 后端项目根: `C:\Users\g\Desktop\linkedknowledge`
- 实体目录: `src/main/java/com/LinkedKnowledge/entity/`
- 已有的实体: `KnowledgeNode.java`, `MindMap.java`, `Tag.java`, `NodeStatus.java`(enum), `NodeType.java`(enum)
- `KnowledgeNode` 已有字段 `id`, `title`, `user_id`, `content`, `type`, `status`, `difficultyLevel`, `parent`(自引用), `children`(自引用 List), `tags`(多对多), `createdAt`, `updatedAt`
- `application.yml` 在 `src/main/resources/application.yml`,有 `spring.jpa.hibernate.ddl-auto: update`

## 4. 后端改动

### 4.1 新建实体:`NodeEdge.java`

位置: `src/main/java/com/LinkedKnowledge/entity/NodeEdge.java`

```java
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
```

### 4.2 新建枚举:`EdgeType.java`

```java
package com.LinkedKnowledge.entity;

public enum EdgeType {
    MANUAL_BUTTON,    // 用户在 UI 上点"连接"按钮建的
    PARSED_LINK,      // 系统从 [[xxx]] 语法解析出来的
    AI_SUGGESTED      // AI 算出来的(此时 edge_status 一开始是 PENDING)
}
```

### 4.3 新建枚举:`EdgeStatus.java`

```java
package com.LinkedKnowledge.entity;

public enum EdgeStatus {
    PENDING,    // 待用户决定(只对 AI_SUGGESTED 有意义)
    ACCEPTED,   // 已接受
    REJECTED    // 已拒绝
}
```

### 4.4 新建实体:`AiEdgeSuggestion.java`

位置: `src/main/java/com/LinkedKnowledge/entity/AiEdgeSuggestion.java`

```java
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
 *
 * 工作流:
 *  1. AI 算 (nodeA, nodeB) 相关 → INSERT AiEdgeSuggestion(status=PENDING)
 *  2. 用户在 UI 看到"AI 觉得 nodeA 应该链接到 nodeB"
 *  3. 用户点接受 → INSERT NodeEdge(edge_type=AI_SUGGESTED, status=ACCEPTED)
 *                 → UPDATE AiEdgeSuggestion(status=ACCEPTED)
 *  4. 用户点拒绝 → UPDATE AiEdgeSuggestion(status=REJECTED)
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
```

### 4.5 新建枚举:`AiSuggestionStatus.java`

```java
package com.LinkedKnowledge.entity;

public enum AiSuggestionStatus {
    PENDING,    // 待用户决定
    ACCEPTED,   // 已接受 → 已建 NodeEdge
    REJECTED,   // 已拒绝
    EXPIRED     // 7 天未处理 → 自动失效(后台定时任务跑)
}
```

### 4.6 新建 Repository

**NodeEdgeRepository.java** (`src/main/java/com/LinkedKnowledge/repository/NodeEdgeRepository.java`):

```java
package com.LinkedKnowledge.repository;

import com.LinkedKnowledge.entity.NodeEdge;
import com.LinkedKnowledge.entity.EdgeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface NodeEdgeRepository extends JpaRepository<NodeEdge, Long> {

    /** 反向链接:查所有 target_node_id = X 且已 accepted 的边 */
    @Query("SELECT e FROM NodeEdge e WHERE e.targetNodeId = :nodeId AND e.edgeStatus = 'ACCEPTED'")
    List<NodeEdge> findAcceptedByTarget(@Param("nodeId") Long nodeId);

    /** 正向链接:查所有 source_node_id = X 且已 accepted 的边 */
    @Query("SELECT e FROM NodeEdge e WHERE e.sourceNodeId = :nodeId AND e.edgeStatus = 'ACCEPTED'")
    List<NodeEdge> findAcceptedBySource(@Param("nodeId") Long nodeId);

    /** 用户的所有 AI 待处理建议边 */
    @Query("SELECT e FROM NodeEdge e WHERE e.edgeStatus = 'PENDING' AND e.edgeType = 'AI_SUGGESTED'")
    List<NodeEdge> findPendingAiEdges();
}
```

**AiEdgeSuggestionRepository.java**:

```java
package com.LinkedKnowledge.repository;

import com.LinkedKnowledge.entity.AiEdgeSuggestion;
import com.LinkedKnowledge.entity.AiSuggestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AiEdgeSuggestionRepository extends JpaRepository<AiEdgeSuggestion, Long> {

    List<AiEdgeSuggestion> findByUserIdAndStatus(Long userId, AiSuggestionStatus status);

    List<AiEdgeSuggestion> findBySourceNodeIdAndStatus(Long sourceNodeId, AiSuggestionStatus status);

    /** 检查是否已有同向建议(避免重复 AI 推荐) */
    boolean existsBySourceNodeIdAndTargetNodeIdAndStatus(
            Long sourceNodeId, Long targetNodeId, AiSuggestionStatus status);
}
```

### 4.7 新建 DTO 和 Controller

**EdgeCreateRequest.java** (`src/main/java/com/LinkedKnowledge/dto/EdgeCreateRequest.java`):

```java
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
```

**NodeEdgeController.java** (`src/main/java/com/LinkedKnowledge/controller/NodeEdgeController.java`):

```java
package com.LinkedKnowledge.controller;

import com.LinkedKnowledge.dto.EdgeCreateRequest;
import com.LinkedKnowledge.entity.NodeEdge;
import com.LinkedKnowledge.entity.EdgeStatus;
import com.LinkedKnowledge.repository.NodeEdgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/edges")
@RequiredArgsConstructor
public class NodeEdgeController {

    private final NodeEdgeRepository edgeRepository;

    /** 建边 */
    @PostMapping
    public ResponseEntity<NodeEdge> create(@RequestBody EdgeCreateRequest req) {
        NodeEdge edge = new NodeEdge();
        edge.setSourceNodeId(req.getSourceNodeId());
        edge.setTargetNodeId(req.getTargetNodeId());
        edge.setEdgeType(req.getEdgeType());
        edge.setSourceText(req.getSourceText());

        // AI 建议的边初始状态是 PENDING,其他类型直接 ACCEPTED
        if (req.getEdgeType() == com.LinkedKnowledge.entity.EdgeType.AI_SUGGESTED) {
            edge.setEdgeStatus(EdgeStatus.PENDING);
        } else {
            edge.setEdgeStatus(EdgeStatus.ACCEPTED);
            edge.setAcceptedAt(LocalDateTime.now());
        }

        return ResponseEntity.ok(edgeRepository.save(edge));
    }

    /** 删边 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        edgeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /** 正向链接 — 节点 X 引用了谁 */
    @GetMapping("/source/{nodeId}")
    public List<NodeEdge> bySource(@PathVariable Long nodeId) {
        return edgeRepository.findAcceptedBySource(nodeId);
    }

    /** 反向链接 — 谁引用了节点 X */
    @GetMapping("/target/{nodeId}")
    public List<NodeEdge> byTarget(@PathVariable Long nodeId) {
        return edgeRepository.findAcceptedByTarget(nodeId);
    }
}
```

### 4.8 ALTER TABLE 兜底脚本

如果 `ddl-auto=update` 没自动建表,手动跑(放 `src/main/resources/db/migration/p0_edge_tables.sql`):

```sql
CREATE TABLE IF NOT EXISTS node_edge (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    source_node_id BIGINT NOT NULL,
    target_node_id BIGINT NOT NULL,
    edge_type VARCHAR(32) NOT NULL,
    edge_status VARCHAR(32) NOT NULL,
    source_text VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    accepted_at DATETIME(6),
    rejected_at DATETIME(6),
    INDEX idx_source (source_node_id),
    INDEX idx_target (target_node_id),
    INDEX idx_status_type (edge_status, edge_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_edge_suggestion (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    source_node_id BIGINT NOT NULL,
    target_node_id BIGINT NOT NULL,
    score DOUBLE NOT NULL,
    suggestion_reason VARCHAR(500),
    user_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    decided_at DATETIME(6),
    INDEX idx_user_status (user_id, status),
    INDEX idx_source (source_node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 5. 前端改动

**本期 P0 不动前端**。只做后端实体 + Repository + Controller。前端接入放到 P1。

## 6. 测试要求

完成后必须验证:

1. 启动后端 `mvn spring-boot:run`,确认日志无 ERROR
2. `SHOW TABLES;` 在 MySQL 客户端看到 `node_edge` 和 `ai_edge_suggestion`
3. 用 curl 测:

```bash
# 建一条边
curl -X POST http://localhost:8080/api/edges \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <你的JWT>" \
  -d '{"sourceNodeId":1,"targetNodeId":2,"edgeType":"MANUAL_BUTTON"}'

# 查正向
curl http://localhost:8080/api/edges/source/1 -H "Authorization: Bearer ..."

# 查反向
curl http://localhost:8080/api/edges/target/2 -H "Authorization: Bearer ..."

# 删
curl -X DELETE http://localhost:8080/api/edges/1 -H "Authorization: Bearer ..."
```

## 7. 不做的事(明确边界)

- **不要**重构 `KnowledgeNode` 的 parent/children(本期内保留作为树形展示用)
- **不要**改 user / jwt / SecurityConfig
- **不要**做 AI 联想(放 P2)
- **不要**做 [[xxx]] 解析(放 P1)
- **不要**写测试用例(只做手工 curl 验证)
- **不要**碰前端
