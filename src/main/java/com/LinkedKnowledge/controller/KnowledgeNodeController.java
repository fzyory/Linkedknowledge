// [框架]    本文件是 KnowledgeNode (知识点节点) 的 REST 控制器, 负责 HTTP 请求入口。
//             路径前缀 /api/nodes, 共暴露 8 个 HTTP 端点 (CRUD + 搜索 + 按类型/根节点查询)。
//             调用链路: HTTP → 本 Controller → KnowledgeNodeService 接口 → Impl → Repository → MySQL。
//             所有响应统一用 common/Result<T> 包装 (成功/失败/消息/数据)。
// [依赖]    common/Result — 统一响应包装类, 包含 code / message / data 三个字段。
//             entity/KnowledgeNode — 实体类, 既做请求 body 也做响应 data。
//             entity/NodeType — 类型枚举 (Controller 自动从 path 字符串转枚举)。
//             service/KnowledgeNodeService — 业务接口 (本文件不直接调 Repository, 保持分层)。
// [框架妙用] (1) @RestController = @Controller + @ResponseBody, 所有方法返回值直接序列化为 JSON,
//              不再需要每个方法加 @ResponseBody。
//             (2) @RequestMapping("/api/nodes") 类级前缀, 方法级 @GetMapping/@PostMapping 等
//              不再重复写 /api/nodes。
//             (3) @RequiredArgsConstructor (Lombok) — 自动生成构造器注入 final 字段,
//              替代 @Autowired 字段注入, 更易测试、更易看出依赖。
//             (4) @PathVariable / @RequestParam / @RequestBody — 三种入参风格:
//              PathVariable 从 URL 路径取, RequestParam 从 query string 取, RequestBody 从 JSON body 取。
//             (5) Spring MVC 自动把 path 上的字符串 "CONCEPT" 转换成 NodeType 枚举 (enums.STRING)。
// [注意]    SecurityConfig 配置 /api/nodes/** 需鉴权, 未登录请求会被 JwtAuthFilter 拦截 401。
//             修改前确认 SecurityConfig.java 的 antMatchers 配置。

package com.LinkedKnowledge.controller;

import com.LinkedKnowledge.common.AuthHelper;
import com.LinkedKnowledge.common.Result;
import com.LinkedKnowledge.entity.KnowledgeNode;
import com.LinkedKnowledge.entity.NodeType;
import com.LinkedKnowledge.service.KnowledgeNodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// [框架]    KnowledgeNodeController 是 KnowledgeNode 实体的 HTTP 入口, 严格走分层:
//             Controller → Service → Repository, 本类不直接调 Repository。
//             8 个端点覆盖 CRUD + 搜索 + 按类型查询 + 根节点查询。
@RestController
@RequestMapping("/api/nodes")
// [框架妙用] @RequiredArgsConstructor: Lombok 自动为 final 字段生成构造器,
//             Spring 启动时通过构造注入 nodeService (推荐方式, 比 @Autowired 字段注入更显式)。
@RequiredArgsConstructor
public class KnowledgeNodeController {

    // [依赖]    业务接口 (不是 Impl), Spring 会注入 Impl Bean。final 字段保证不可变。
    private final KnowledgeNodeService nodeService;

    // POST /api/nodes — 创建节点
    // [入口]    POST /api/nodes — 创建一个新 KnowledgeNode 节点。
    //             触发方: 前端"新建节点"表单提交, 或外部脚本导入数据。
    // [数据流]  HTTP body (JSON) → Jackson 反序列化成 KnowledgeNode (id 字段可空, JPA 自动生成)
    //          → nodeService.createNode() (内部走 Service → Repository.save())
    //          → 返回保存后的节点 (含自动生成的 id)。
    // [参数]    node: 请求体, JSON 格式, 字段匹配 KnowledgeNode (id 可空)。
    // [返回]    Result<KnowledgeNode> — 包含成功创建的节点, 含自动生成的 id。
    // [依赖]    common/Result.success(...) — 包装成功响应 (code=200, message=ok, data=node)。
    @PostMapping
    public Result<KnowledgeNode> createNode(@RequestBody KnowledgeNode node) {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        node.setUserId(userId);
        node.setId(null);
        return Result.success(nodeService.createNode(node));
    }

    // GET /api/nodes/{id} — 查询单个节点
    // [入口]    GET /api/nodes/{id} — 按主键查询单个节点。
    //             触发方: 前端编辑页加载、详情页渲染。
    // [数据流]  URL path /{id} → @PathVariable Long id → nodeService.getNodeById(id)
    //          → Repository.findById(id) → Optional<KnowledgeNode> → 取出值或抛 404。
    // [参数]    id: 主键 (Long, MySQL AUTO_INCREMENT)。
    // [返回]    Result<KnowledgeNode> — 找到返回节点, 找不到时由 GlobalExceptionHandler 返 404。
    @GetMapping("/{id}")
    public Result<KnowledgeNode> getNode(@PathVariable Long id) {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        return Result.success(nodeService.getNodeById(id, userId));
    }

    // GET /api/nodes — 查询所有节点
    // [入口]    GET /api/nodes — 列出所有节点 (无分页, 数据量大时慎用)。
    //             触发方: 管理后台全列表展示、调试。
    // [数据流]  → nodeService.getAllNodes() → Repository.findAll() → 全部节点列表。
    // [返回]    Result<List<KnowledgeNode>>。
    // [注意]    实际生产建议加分页参数 ?page=0&size=20, 当前实现返回全表,
    //             数据量 > 10000 时会卡。Service 层目前也未实现 Pageable。
    @GetMapping
    public Result<List<KnowledgeNode>> getAllNodes() {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        return Result.success(nodeService.getAllNodes(userId));
    }

    // GET /api/nodes/search?keyword=xxx — 按标题搜索
    // [入口]    GET /api/nodes/search?keyword=xxx — 按 title 字段模糊搜索。
    //             触发方: 前端搜索框输入关键词, 实时检索节点。
    // [数据流]  query string keyword → @RequestParam String keyword
    //          → nodeService.searchByTitle(keyword) → Repository.findByTitleContainingIgnoreCase。
    // [参数]    keyword: 搜索关键词, 大小写不敏感, 走 MySQL LIKE '%keyword%'。
    // [返回]    Result<List<KnowledgeNode>> — 匹配 title 的节点列表 (可能为空)。
    // [注意]    URL 必须转义特殊字符 (% _ 等), 否则可能引发 SQL 注入风险或 LIKE 匹配错乱。
    @GetMapping("/search")
    public Result<List<KnowledgeNode>> searchNodes(@RequestParam String keyword) {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        return Result.success(nodeService.searchByTitle(keyword, userId));
    }

    // GET /api/nodes/type/{type} — 按类型查询
    // [入口]    GET /api/nodes/type/{type} — 按节点类型枚举查询。
    //             触发方: 前端按类型筛选 (CONCEPT/EXAMPLE/EXERCISE/REFERENCE 分类展示)。
    // [数据流]  path /{type} → Spring 自动把字符串转 NodeType 枚举 (因为枚举用 STRING 存储)
    //          → nodeService.getNodesByType(type) → Repository.findByType(type)。
    // [参数]    type: NodeType 枚举, URL 写字符串即可, 如 /api/nodes/type/CONCEPT。
    //             非法枚举值 Spring 直接返 400 (MethodArgumentTypeMismatchException)。
    // [返回]    Result<List<KnowledgeNode>> — 指定类型的所有节点。
    @GetMapping("/type/{type}")
    public Result<List<KnowledgeNode>> getNodesByType(@PathVariable NodeType type) {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        return Result.success(nodeService.getNodesByType(type, userId));
    }

    // GET /api/nodes/roots — 查询顶级节点
    // [入口]    GET /api/nodes/roots — 查询所有 parent = null 的顶级节点。
    //             触发方: 前端首页树形结构根节点展示。
    // [数据流]  → nodeService.getRootNodes() → Repository.findByParentIsNull() → 顶级节点列表。
    // [返回]    Result<List<KnowledgeNode>> — 没有父节点的节点 (树的根)。
    @GetMapping("/roots")
    public Result<List<KnowledgeNode>> getRootNodes() {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        return Result.success(nodeService.getRootNodes(userId));
    }

    // PUT /api/nodes/{id} — 更新节点
    // [入口]    PUT /api/nodes/{id} — 更新指定节点 (全字段覆盖)。
    //             触发方: 前端"编辑节点"表单保存。
    // [数据流]  path id + body JSON → nodeService.updateNode(id, node)
    //          → Repository 查旧节点 → 字段赋值 → save() (JPA 自动判定 INSERT or UPDATE)。
    //          → 触发 @PreUpdate 钩子刷新 updatedAt。
    // [参数]    id: 要更新的节点主键; node: 新数据 (全字段覆盖, 部分字段更新需前端传完整对象)。
    // [返回]    Result<KnowledgeNode> — 更新后的节点。
    // [注意]    PUT 是全量替换, PATCH 才是部分更新。当前实现按 PUT 全量处理。
    @PutMapping("/{id}")
    public Result<KnowledgeNode> updateNode(@PathVariable Long id,
                                            @RequestBody KnowledgeNode node) {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        return Result.success(nodeService.updateNode(id, node, userId));
    }

    // DELETE /api/nodes/{id} — 删除节点
    // [入口]    DELETE /api/nodes/{id} — 删除指定节点 (级联删除所有子节点)。
    //             触发方: 前端"删除节点"按钮 (通常带二次确认)。
    // [数据流]  path id → nodeService.deleteNode(id) → Repository.deleteById(id)
    //          → KnowledgeNode.@OneToMany(cascade=ALL) 自动级联删除 children。
    // [参数]    id: 要删除的节点主键。
    // [返回]    Result<Void> — 200 OK, 无 data。
    // [注意]    级联删除会删掉所有子节点 (cascade = ALL), 不可恢复, 慎用!
    //             生产环境建议做软删除 (status = ARCHIVED) 而非物理 DELETE。
    @DeleteMapping("/{id}")
    public Result<Void> deleteNode(@PathVariable Long id) {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        nodeService.deleteNode(id, userId);
        return Result.success();
    }
}
