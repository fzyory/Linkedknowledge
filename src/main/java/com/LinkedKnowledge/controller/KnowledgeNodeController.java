package com.LinkedKnowledge.controller;

import com.LinkedKnowledge.common.Result;
import com.LinkedKnowledge.entity.KnowledgeNode;
import com.LinkedKnowledge.entity.NodeType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nodes")
@RequiredArgsConstructor
public class KnowledgeNodeController {

    private final KnowledgeNodeService nodeService;

    // POST /api/nodes — 创建节点
    @PostMapping
    public Result<KnowledgeNode> createNode(@RequestBody KnowledgeNode node) {
        return Result.success(nodeService.createNode(node));
    }

    // GET /api/nodes/{id} — 查询单个节点
    @GetMapping("/{id}")
    public Result<KnowledgeNode> getNode(@PathVariable Long id) {
        return Result.success(nodeService.getNodeById(id));
    }

    // GET /api/nodes — 查询所有节点
    @GetMapping
    public Result<List<KnowledgeNode>> getAllNodes() {
        return Result.success(nodeService.getAllNodes());
    }

    // GET /api/nodes/search?keyword=xxx — 按标题搜索
    @GetMapping("/search")
    public Result<List<KnowledgeNode>> searchNodes(@RequestParam String keyword) {
        return Result.success(nodeService.searchByTitle(keyword));
    }

    // GET /api/nodes/type/{type} — 按类型查询
    @GetMapping("/type/{type}")
    public Result<List<KnowledgeNode>> getNodesByType(@PathVariable NodeType type) {
        return Result.success(nodeService.getNodesByType(type));
    }

    // GET /api/nodes/roots — 查询顶级节点
    @GetMapping("/roots")
    public Result<List<KnowledgeNode>> getRootNodes() {
        return Result.success(nodeService.getRootNodes());
    }

    // PUT /api/nodes/{id} — 更新节点
    @PutMapping("/{id}")
    public Result<KnowledgeNode> updateNode(@PathVariable Long id,
                                            @RequestBody KnowledgeNode node) {
        return Result.success(nodeService.updateNode(id, node));
    }

    // DELETE /api/nodes/{id} — 删除节点
    @DeleteMapping("/{id}")
    public Result<Void> deleteNode(@PathVariable Long id) {
        nodeService.deleteNode(id);
        return Result.success();
    }
}
