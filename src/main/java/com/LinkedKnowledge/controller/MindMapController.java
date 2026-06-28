package com.LinkedKnowledge.controller;

import com.LinkedKnowledge.common.Result;
import com.LinkedKnowledge.dto.MindMapGenerateRequest;
import com.LinkedKnowledge.dto.MindMapNode;
import com.LinkedKnowledge.service.MindMapService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mindmap")
@RequiredArgsConstructor
public class MindMapController {

    private final MindMapService mindMapService;

    /**
     * POST /api/mindmap/generate
     * 请求体: { "topic": "Java 多线程", "context": "可选上下文" }
     * 返回: 树形 MindMapNode
     */
    @PostMapping("/generate")
    public Result<MindMapNode> generate(@RequestBody MindMapGenerateRequest request) {
        if (request.getTopic() == null || request.getTopic().isBlank()) {
            return Result.error(400, "topic 不能为空");
        }
        MindMapNode root = mindMapService.generateMindMap(request.getTopic(), request.getContext());
        return Result.success(root);
    }
}