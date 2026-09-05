# linkedknowledge 后端架构

> Spring Boot 3.3.5 + JPA + MySQL + Redis + JWT

## 包结构

```
com.LinkedKnowledge/
├── LinkedKnowledgeApplication.java   # 启动类
├── common/                            # 通用工具
│   ├── JwtUtil.java                   # JWT 生成/验证
│   ├── JwtAuthFilter.java             # 鉴权过滤器
│   ├── Result.java                    # 统一响应包装
│   ├── LlmClient.java                 # AI 调用(暂未用)
│   └── GlobalExceptionHandler.java    # 全局异常处理
├── config/                            # 配置
│   ├── SecurityConfig.java            # Spring Security
│   └── RedisConfig.java
├── controller/                        # REST API
│   ├── AuthController.java
│   ├── MindMapController.java         # 核心
│   ├── KnowledgeNodeController.java   # 暂未用
│   └── HelloController.java
├── dto/                               # 请求/响应 DTO
│   ├── MindMapFullData.java           # 完整导图
│   ├── MindMapNode.java               # 单个节点
│   └── MindMapGenerateRequest.java
├── entity/                            # JPA 实体
│   ├── User.java
│   ├── MindMap.java                   # 导图实体
│   ├── KnowledgeNode.java
│   ├── NodeType.java                  # 枚举
│   ├── NodeStatus.java
│   └── Tag.java
├── repository/                        # JPA 仓库
│   ├── UserRepository.java
│   ├── MindMapRepository.java
│   └── KnowledgeNodeRepository.java
├── service/                           # 业务逻辑
│   ├── UserService.java / Impl
│   └── MindMapService.java / Impl      # 核心
└── demo/
    └── CollectionDemo.java            # 演示
```

## 数据模型

### mind_map 表
| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | 主键 |
| title | VARCHAR(200) | 导图标题 |
| topic | VARCHAR(500) | 主题 |
| context | TEXT | 上下文(可选) |
| tree_json | LONGTEXT | **完整数据 JSON**(nodeData + arrows + summaries + direction) |
| user_id | BIGINT | 所属用户 |
| created_at | DATETIME | |
| updated_at | DATETIME | |

### tree_json 格式
```json
{
  "nodeData": {
    "id": "root", "topic": "Java 多线程", "type": "ROOT",
    "x": 100, "y": 50,
    "children": [
      { "id": "n1", "topic": "synchronized", "x": 300, "y": 30, "children": [] },
      ...
    ]
  },
  "arrows": [
    { "id": "a1", "from": "n1", "to": "n2", "label": "锁升级",
      "delta1": {"x":0,"y":0}, "delta2": {"x":0,"y":0} }
  ],
  "summaries": [],
  "direction": 0
}
```

## API 设计

### 响应统一格式
```java
public class Result<T> {
    int code;      // 200=成功,401=未登录,404=不存在,500=错误
    String message;
    T data;
}
```

### 主要端点
| Method | Path | Body | 用途 |
|---|---|---|---|
| POST | /api/auth/register | {username, password, email} | 注册 |
| POST | /api/auth/login | {username, password} | 登录(Set-Cookie) |
| POST | /api/auth/logout | | 登出(Redis 黑名单) |
| GET | /api/auth/me | | 当前用户 |
| GET | /api/mindmap/list | | 我的列表 |
| GET | /api/mindmap/search?keyword= | | 搜索 |
| GET | /api/mindmap/{id} | | 单条 |
| GET | /api/mindmap/{id}/tree | | **完整数据** |
| POST | /api/mindmap/generate | {topic, context} | AI 生成 |
| PUT | /api/mindmap/{id} | {title? 或 nodeData+arrows+...} | 重命名/更新 |
| DELETE | /api/mindmap/{id} | | 删除 |

## 鉴权

**机制**:JWT + Redis 黑名单
1. `POST /login` 成功 → 响应 `Set-Cookie: token=...`
2. 后续请求自动带 Cookie
3. `JwtAuthFilter` 解析 token → 拿 userId → 写 SecurityContext
4. 登出:把 token 加 Redis 黑名单(7 天过期),cookie maxAge=0
5. `parseToken` 检查黑名单 → 拒绝

**关键文件**:
- `common/JwtUtil.java` — generate/validate/getUserId/getExpiration
- `common/JwtAuthFilter.java` — 解析 + 写 SecurityContext
- `config/SecurityConfig.java` — SecurityFilterChain 配置

## 关键设计

### 1. tree_json 完整持久化
- 不用单独建 node 表,JSON 存整棵树
- 优点:读一次 getTree 拿全部,写一次 PUT 更新
- 缺点:树太大(>1MB)时性能差 — 现状够用

### 2. parseTreeJson 自动算 type
- 后端不依赖前端传 type,递归根据 children 是否空设 ROOT/BRANCH/LEAF
- Controller 强制把根节点设 ROOT

### 3. 权限校验复用
- 所有需要 userId 的接口都从 cookie 取
- `requireUserId(HttpServletRequest)` helper

### 4. MindMapNode Integer 字段
- `Integer x, y` 而非 `int` — 允许 null,兼容老数据
- `Integer direction` — 允许 null

## 已知限制

1. **LlmClient 未配 key** — `generate` 端点走不通
2. **KnowledgeNode 实体未用** — 旧的 node 表,前端不用
3. **tree_json 体积限制** — 太大时 MySQL LONGTEXT 仍能存但查询慢
4. **JWT secret 硬编码** — 改了 `application.yml`,但生产应放 Vault
