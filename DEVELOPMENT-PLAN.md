# LinkedKnowledge 项目补完计划

> 原则：**你自己写代码，我不写**。每个任务都标了「学完这一节再做下一节」。

---

## 总览：4 个阶段，14 个任务

| 阶段 | 主题 | 任务数 | 预计耗时 |
|---|---|---|---|
| **第一阶段** | 数据安全 / 用户隔离 | 3 | ~3h |
| **第二阶段** | 表单验证 / DTO | 3 | ~2h |
| **第三阶段** | 数据库迁移（Flyway） | 4 | ~3h |
| **第四阶段** | 工程化（配置/测试） | 4 | ~4h |

---

# 第一阶段：数据安全 / 用户隔离 ⚠️ 必修

> **为什么先做**：现在任何登录用户能改/删别人的数据，是**生产级安全漏洞**。

## 任务 1.1：KnowledgeNode 加 userId 字段

**学习目标**：
- 理解 JPA 实体字段加 DB 列的完整流程
- 理解 ddl-auto: update 行为（不加 schema migration）

**步骤**：

1. 打开 `entity/KnowledgeNode.java`
2. 加字段：
   ```java
   @Column(name = "user_id", nullable = false)
   private Long userId;
   ```
3. 重启 Spring Boot，看 Hibernate 自动生成的 ALTER TABLE 语句
4. 用 MySQL 客户端验证 `knowledge_node` 表多了 `user_id` 列

**验证**：
- 表结构里有 user_id
- 没改任何业务代码，能正常启动

**别急着**进入 1.2——先把字段加好。

---

## 任务 1.2：JwtAuthFilter 把 userId 塞进 SecurityContext

**学习目标**：
- 理解 Spring Security 的 `SecurityContextHolder`
- 理解 `Authentication.getDetails()` 怎么用

**当前状态**：你的 `JwtAuthFilter` 已经把 username 塞进 SecurityContext 了——**但没塞 userId**——Controller 取不到。

**步骤**：

1. 看 `JwtAuthFilter.java` 第 79-82 行
2. 思考：现在 `authentication` 只有 username——怎么把 userId 也带上？
3. 提示：
   - `authentication.setDetails(userId)` 现在写的是「setDetails(userId)」——这里 details 是 Object
   - Controller 取的时候 `authentication.getDetails()` 也是 Object
   - 你可以**改成存一个 Map**——或者**自定义 Authentication**
 
**验证**：
- 在任意 Controller 里 `@AuthenticationPrincipal` 或从 SecurityContext 拿到 userId

**别急着**进入 1.3——先想清楚怎么把 userId 传到 Controller 层。

---

## 任务 1.3：KnowledgeNodeService 加 user 隔离

**学习目标**：
- 理解「业务层做数据过滤」而不是「数据库层做」的设计取舍

**步骤**：

1. 改 `KnowledgeNodeService` 接口——`createNode`、`updateNode`、`deleteNode`、`getNodeById` 都加 `userId` 参数
2. 改 `KnowledgeNodeServiceImpl`：
   - `createNode(node, userId)` —— 创建前 `node.setUserId(userId)`
   - `getNodeById(id, userId)` —— 查完判断 `node.getUserId().equals(userId)`，不是就抛「无权限」
   - `updateNode(id, node, userId)` —— 同样校验
   - `deleteNode(id, userId)` —— 同样校验
3. 改 `KnowledgeNodeController` —— 从 SecurityContext 取 userId，调用 Service

**验证**：
- 用户 A 创建节点 → 用户 B 用 B 的 token 访问 `/api/nodes/{A的id}` → 返回「无权限」或 403
- 用 curl 两个不同用户测试

**学完这一节**：
- 你应该能解释「为什么不在 Controller 层做 user 过滤」——**因为 Service 才是业务层**
- 你应该能解释「为什么不在 SQL 层做」（MySQL 视图 / Row-level Security）——**那是另一种设计**

---

# 第二阶段：表单验证 / DTO

> **为什么第二**：数据安全修了，用户体验也要跟上。现在 register 可以传空 username、垃圾 email。

## 任务 2.1：定义 Request DTO

**学习目标**：
- 理解「Entity 不是 DTO」的设计原则
- 理解 `@Valid` + `jakarta.validation` 注解

**步骤**：

1. 在 `dto/` 下新建文件 `RegisterRequest.java`（注意：你的 AuthController 把 RegisterRequest 放在了 controller 文件里——**现在拆出来**）
2. 字段：
   ```java
   @NotBlank
   @Size(min=3, max=20)
   private String username;

   @NotBlank
   @Size(min=6, max=64)
   private String password;

   @NotBlank
   @Email
   private String email;
   ```
3. 在 AuthController 把请求参数改成 `@Valid @RequestBody RegisterRequest`
4. **别忘了**加 `spring-boot-starter-validation` 依赖

**验证**：
- 用 curl 传空 username → 应该返回 400 而不是 500
- 用 curl 传垃圾邮箱 → 应该返回 400

---

## 任务 2.2：定义 Response DTO

**学习目标**：
- 理解「响应里绝不暴露 Entity」
- 现在 `register` 返回 `Result<User>` —— User 里有 password 字段——**用 setPassword(null) 是 hack**

**步骤**：

1. 在 `dto/` 下新建 `UserResponse.java`
2. 字段：`id`、`username`、`email`、`createdAt`（**没有 password**）
3. AuthController 的 register / login / me 都返回 `UserResponse`
4. Service 加 `toResponse(User)` 转换方法

**验证**：
- 注册返回的 JSON 里有 `id`、`username`、`email`，**没有 `password` 字段**

---

## 任务 2.3：把 UserService.register 改成 email 也校验

**学习目标**：
- 理解「业务层校验 + DB唯一约束 = 双保险」

**步骤**：

1. 看现在 `UserServiceImpl.register` 只查 `existsByUsername`
2. 改成也查 `existsByEmail`
3. 思考：抛什么异常？新建 `BusinessException` 吗？还是用 `RuntimeException` + 自定义 message？

**验证**：
- 用 curl 注册相同 email → 返回「邮箱已被注册」而不是 500 SQL 异常

---

# 第三阶段：Flyway 数据库迁移

> **为什么第三**：前两个阶段还没改 schema。等 schema 稳定后，引入 Flyway 是**最佳时机**。

## 任务 3.1：关 ddl-auto，加 Flyway 依赖

**学习目标**：
- 理解「为什么生产不能靠 Hibernate 自动建表」

**步骤**：

1. 加依赖（pom.xml）：
   ```xml
   <dependency>
     <groupId>org.flywaydb</groupId>
     <artifactId>flyway-core</artifactId>
   </dependency>
   <dependency>
     <groupId>org.flywaydb</groupId>
     <artifactId>flyway-mysql</artifactId>
   </dependency>
   ```
2. `application.yml` 改：
   ```yaml
   spring:
     jpa:
       hibernate:
         ddl-auto: validate   # 从 update 改成 validate
     flyway:
       enabled: true
       baseline-on-migrate: true
   ```
3. 重启，看 Flyway 在控制台输出「Creating Schema History table」「Migrating schema » to version 1」
4. 第一次启动如果报错（因为已经有表），**用 baseline-on-migrate**

**验证**：
- 数据库多了 `flyway_schema_history` 表
- `knowledge_node` 等表**没被删**（Flyway 知道已经有表了）

---

## 任务 3.2：写 V1__init.sql

**学习目标**：
- 理解「写数据库迁移脚本」的规范（V1__ 开头、单向）

**步骤**：

1. 在 `src/main/resources/db/migration/` 下建 `V1__init.sql`
2. 把所有 6 个实体的建表 SQL 写出来——**别用 Hibernate 生成，自己手写**
3. 用 mysql 客户端对比你写的 SQL 和 Hibernate 生成的——**有哪些不一样？**

**思考题**：
- 你的 `MindMap.treeJson` 用了 `@Lob` + `LONGTEXT` ——手写 SQL 时该怎么写？
- 索引要不要加？FK 约束要不要加？cascade 怎么写？

---

## 任务 3.3：写 V2__add_user_id_to_knowledge_node.sql

**学习目标**：
- 理解「增量迁移」

**步骤**：

1. `V2__add_user_id_to_knowledge_node.sql`：
   ```sql
   ALTER TABLE knowledge_node ADD COLUMN user_id BIGINT NOT NULL DEFAULT 0;
   CREATE INDEX idx_knowledge_node_user_id ON knowledge_node(user_id);
   ```
2. 重启，看 Flyway 自动执行 V2
3. **注意**：你第一阶段已经手动加了 user_id——所以 V2 应该**幂等**或者你得做 baseline

**思考题**：
- 已有数据怎么办？（你这个项目 dev 数据库可以删了重建，但生产怎么办？）
- 答案：写迁移脚本前**先把数据导出/备份**——但这是高级话题，先不展开

---

## 任务 3.4：把 entity 加 `@Index` 注解 + 关键字段索引

**学习目标**：
- 理解「性能优化从索引开始」

**步骤**：

1. User 加索引：`@Table(name="users", indexes = {@Index(columnList="username"), @Index(columnList="email")})`
2. KnowledgeNode 加：`@Index(columnList="title")`、`@Index(columnList="user_id")`、`@Index(columnList="parent_id")`
3. MindMap 加：`@Index(columnList="user_id")`
4. **别忘了**写 V3 迁移脚本同步

**验证**：
- mysql 客户端 `SHOW INDEX FROM knowledge_node;` 看到新索引

---

# 第四阶段：工程化

> **为什么最后**：前三个阶段是必须的功能/安全——这阶段是「让项目更专业」。

## 任务 4.1：JWT secret 从配置读

**步骤**：

1. `application.yml`：
   ```yaml
   jwt:
     secret: ${JWT_SECRET:linkedknowledge-jwt-secret-key-256bits!!}
     expiration-ms: 86400000
   ```
3. `JwtUtil` 加 `@ConfigurationProperties(prefix = "jwt")`
4. **生产环境**用 `.env` 文件或环境变量覆盖

**验证**：
- 设 `JWT_SECRET=abc` 环境变量重启——token签名变了，旧 token 失效

---

## 任务 4.2：数据库密码从环境变量读

**步骤**：

1. `application.yml`：
   ```yaml
   spring:
     datasource:
       password: ${DB_PASSWORD:123456}
   ```
2. `.env` 文件加 `DB_PASSWORD=真实密码`——`.gitignore` 加 `.env`

**验证**：
- 启动脚本传入不同 `DB_PASSWORD`——连接不同数据库

---

## 任务 4.3：写第一个单元测试

**学习目标**：
- 理解「先写测试再写代码」（TDD）的边界——对于 legacy 代码用「补测试」

**步骤**：

1. 加 `spring-boot-starter-test` 依赖（你已经有了）
2. 写 `UserServiceTest.java`：
   - `@SpringBootTest` + `@Transactional`
   - 测试 register 正常流程
   - 测试 username 已存在 → 抛异常
3. **思考**：Service 测试需要 DB 吗？用 H2 in-memory 还是直接用 MySQL？

**验证**：
- `mvn test` 跑通

---

## 任务 4.4：加 Knife4j (Swagger UI)

**学习目标**：
- 理解「API 文档」的重要性

**步骤**：

1. 加依赖：
   ```xml
   <dependency>
     <groupId>com.github.xiaoymin</groupId>
     <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
     <version>4.4.0</version>
   </dependency>
   ```
2. 配 application.yml
3. Controller 加 `@Tag` `@Operation` 注解
4. 启动后访问 `http://localhost:8080/doc.html`

**验证**：
- 能在 Knife4j UI 看到所有 API
- 能直接在线测试登录/注册

---

# 进度跟踪

每完成一个任务，在下面打勾：

## 第一阶段
- [ ] 1.1 KnowledgeNode 加 userId
- [ ] 1.2 JwtAuthFilter 塞 userId 到 SecurityContext
- [ ] 1.3 KnowledgeNodeService 加 user 隔离

## 第二阶段
- [ ] 2.1 RegisterRequest DTO + @Valid
- [ ] 2.2 UserResponse DTO（去 password）
- [ ] 2.3 UserService.register 加 email 校验

## 第三阶段
- [ ] 3.1 关 ddl-auto + 加 Flyway
- [ ] 3.2 写 V1__init.sql
- [ ] 3.3 写 V2__add_user_id
- [ ] 3.4 entity 加索引

## 第四阶段
- [ ] 4.1 JWT secret 配置化
- [ ] 4.2 DB 密码环境变量
- [ ] 4.3 第一个单元测试
- [ ] 4.4 Knife4j API 文档

---

# 学习节奏建议

- **每天 1-2 个任务**，**每个任务前先看 5-10 分钟相关概念**
- **遇到错就停下来**——不要堆错下次一起查
- **每个任务完成后写一段日志**——记录「学到了什么」/「卡在哪」/「下次怎么避免」
- **每个阶段完成后**——做一个**小 demo / 验证脚本**确认理解

**最后**——这个计划**不是死的**——任何时候觉得「这里要先做别的」都可以调。