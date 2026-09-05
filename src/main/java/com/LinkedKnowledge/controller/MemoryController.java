package com.LinkedKnowledge.controller;

import com.LinkedKnowledge.common.AuthHelper;
import com.LinkedKnowledge.common.Result;
import com.LinkedKnowledge.dto.MemoryChatResponse;
import com.LinkedKnowledge.entity.MemoryFact;
import com.LinkedKnowledge.entity.MemoryMessage;
import com.LinkedKnowledge.service.MemoryService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/memory")
@RequiredArgsConstructor
public class MemoryController {

    private final MemoryService memoryService;

    @GetMapping("/facts")
    public Result<List<MemoryFact>> facts(@RequestParam(defaultValue = "") String q) {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(memoryService.facts(userId, q));
    }

    @GetMapping("/chat")
    public Result<List<MemoryMessage>> history() {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(memoryService.history(userId));
    }

    @PostMapping("/chat")
    public Result<MemoryChatResponse> chat(@RequestBody ChatRequest body) {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        try {
            return Result.success(memoryService.chat(userId, body == null ? null : body.getMessage(),
                    body == null ? null : body.getNodeId()));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/chat")
    public Result<Void> clear() {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        memoryService.clearChat(userId);
        return Result.success();
    }

    @Data
    public static class ChatRequest {
        private String message;
        private Long nodeId;
    }
}
