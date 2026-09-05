# linkedknowledge 后端开发对话实录

> 时间:2026-05 ~ 2026-06-29
> 角色:Hermes + Fox
> 栈:Spring Boot 3.3.5 + JPA + JWT + MySQL + Redis

---

## Phase 0:已有项目

Fox 接手时,后端已经基本搭好:
- `AuthController` 登录/注册/me/logout
- `MindMapController` 已有 generate/list/getById/getTree
- `JwtUtil` / `JwtAuthFilter` 鉴权
- `SecurityConfig` Spring Security 配置

但有几个 **已存在的 bug** 没发现:
1. `@PathVariable Long id` 没指定 name — Spring 反射拿不到参数名
2. `mvn dependency:build-classpath` 用 `includeScope=compile` 漏掉 runtime 依赖
3. `spring-boot-loader` 没在 m2 仓库

---

## Phase 1:首次启动跑通

**问题**:
```
java.lang.NoClassDefFoundError: Could not initialize class sun.security.ssl.SSLContextImpl
```

**诊断**:Maven 自身 SSL 配置有 bug,无法下载依赖。

**解决**:用项目自带的 `mvnw.cmd`(Maven Wrapper),它会下载独立 Maven,绕开系统 SSL 问题。

## Phase 2:启动 PropertiesLauncher 找不到

**问题**:
```
Error: Could not find or load main class
org.springframework.boot.loader.launch.PropertiesLauncher
```

**诊断**:spring-boot-loader jar 没下载。

**解决**:
```bash
./mvnw.cmd dependency:get -Dartifact=org.springframework.boot:spring-boot-loader:3.3.5
```

## Phase 3:jjwt KeysBridge 找不到

**问题**:
```
io.jsonwebtoken.lang.UnknownClassException: Unable to load class named
[io.jsonwebtoken.impl.security.KeysBridge]
```

**诊断**:jjwt-api 在 compile scope,但 KeysBridge 在 jjwt-impl(runtime scope)。
maven 默认 build-classpath 只拉 compile。

**解决**:
```bash
mvn dependency:build-classpath -DincludeScope=runtime -Dmdep.outputFile=target/cp.txt
```

## Phase 4:PathVariable "Name for argument not specified"

**问题**:
```
Name for argument of type [Long] not specified, and parameter name information
not available via reflection. Ensure that the compiler uses the '-parameters' flag.
```

**诊断**:Spring 通过 `-parameters` 编译标志读取参数名。Fox 之前手动 javac 没用这个标志。

**解决**:`compile.bat` / javac 命令加 `-parameters`。

## Phase 5:前端 /me 接口 401

**问题**:前端路由守卫 `authApi.me()` 失败。

**诊断**:
- 前端 axios 响应拦截器 `response => response.data` — 所以 r 已经是后端 Result.data
- 后端 `@PathVariable` 默认要参数名,没加 `-parameters` 时 Spring 反射失败
- 我在 MindMapController 把 `@PathVariable Long id` 全改为 `@PathVariable("id") Long id`

## Phase 6:Fox 问"前端 /me 怎么 401"

我之前没找到 -parameters 标志,直接看代码:
- 前端 api/index.js 用 `withCredentials: true` 让 Cookie 一起发
- 浏览器 SameSite 规则: 默认 Lax,后端 Set-Cookie `HttpOnly` + `Path=/` 跨域 OK
- Vite 代理 `/api/*` 到 `http://localhost:8080` — 跨域 Cookie 默认浏览器会丢掉

**解决**:确认 Vite 代理配好了,前后端都用 `localhost` 同一端口即可。

## Phase 7:Fox 要 XMind 风格功能 → 加 3 个 API

**新增**:
- `DELETE /api/mindmap/{id}` — 删除(权限校验)
- `PUT /api/mindmap/{id}` — 重命名 / 完整更新
- `GET /api/mindmap/search?keyword=xxx` — 搜索

**实现**:
- `@RequestParam(value = "keyword", required = false)` 显式命名(同上,因 -parameters)
- `delete` 方法:`getById` 复用权限校验 → 删
- `update`:分两种 — 改 title(只 rename)或 改 tree(nodeData)
- search:trim 关键词,空关键词返回所有

## Phase 8:Fox 要完整保存(arrows/summaries)→ MindMapFullData DTO

**问题**:之前 `treeJson` 只存 nodeData,丢失 arrows/summaries/direction。

**解决**:
- 新建 `MindMapFullData` DTO: `{ nodeData, arrows, summaries, direction }`
- `parseTreeJson` 兼容新旧格式
- `getTree` 返回 MindMapFullData(用 Map 序列化避免 Jackson 泛型擦除)
- `update` 接收 MindMapUpdateRequest(支持 title / nodeData / arrows / summaries)
- 新加 `saveFullData` Service 方法,只更新 treeJson 字段

**踩坑**:
- Jackson 泛型擦除:`Result<MindMapFullData>` 序列化失败
- 解决:在 Controller 改成 `Result<Object>`,手动 build Map

## Phase 9:Fox 要前端 Konva 自由画布 → MindMapNode 加 x/y

**问题**:前端 Konva 需要存节点 x/y 坐标,但 MindMapNode DTO 没这字段,Jackson 静默忽略。

**解决**:
- `MindMapNode` 加 `Integer x` / `Integer y` 字段
- 用 `Integer` 而非 `int` — 允许 null(老数据没 x/y 时不报错)

**踩坑**:
- Lombok `@Data` 自动生成 getter/setter,但 Jackson 需要 getter — 自动就有
- 之前 `@PathVariable` 同样问题不是 Lombok 是 javac 标志

## Phase 10:Fox 关心重复 ctrl+c 杀主进程

**Phase 1 时:** 我帮 Fox 在 translation 项目里用 pynput mouseup 钩子发 ctrl+c。
**后端相关**:这跟 Spring Boot 无关,但 pynput 发的 ctrl+c 在 PowerShell 里转发 SIGINT,杀 Python 进程。

## Phase 11:文档化

**本目录**:把开发过程整理成文档
- `docs/DEVELOPMENT-LOG.md` (本文)
- `docs/ARCHITECTURE.md` — 架构分析
- 代码注释:关键文件加 Javadoc

---

## 关键经验教训

### 1. 编译参数 -parameters 必须加
Spring 的 `@PathVariable` / `@RequestParam` 反射拿参数名需要 javac 的 `-parameters` 标志。`compile.bat` 必须包含,否则所有带 `@PathVariable` 的接口 500。

### 2. Maven dependency:build-classpath 默认只拉 compile
jjwt-impl/jjwt-jackson 等是 `runtime` scope,默认会漏。**永远用 `includeScope=runtime`**。

### 3. spring-boot-loader 是个独立 jar
mvn 拉 jar 时不会自动拉 `spring-boot-loader`,但 `PropertiesLauncher` 在它里面。PropertiesLauncher 模式比 java -jar 灵活(可指定主类),值得用。

### 4. Jackson 泛型擦除
`Result<T>` 序列化,T 在编译期擦除,Jackson 看到的是 Result<Object>。如果 T 是自定义 DTO,可能丢字段。
**解决:Controller 用 `Result<Object>`,手动 build Map**。

### 5. Lombok @Data 的局限
- 不会生成有参构造(用 `@AllArgsConstructor`)
- 不会生成无参构造(用 `@NoArgsConstructor`)
- 不会自动加 builder(用 `@Builder`)

### 6. 增量编译
单文件修改不需要全项目重新编译:
```bash
javac -d target/classes -classpath "target/classes;..." 单个文件.java
```
只要依赖稳定即可。

### 7. 字段名是 API
后端 DTO 字段名是 API contract,改字段 = 破坏前端兼容。
- 删字段:慎,需前配合
- 加字段:安全,默认 null
- 重命名字段:不推荐,要前端同步
