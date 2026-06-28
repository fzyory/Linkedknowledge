package com.LinkedKnowledge.controller;

import com.LinkedKnowledge.common.JwtUtil;
import com.LinkedKnowledge.common.Result;
import com.LinkedKnowledge.dto.MindMapGenerateRequest;
import com.LinkedKnowledge.dto.MindMapNode;
import com.LinkedKnowledge.entity.MindMap;
import com.LinkedKnowledge.service.MindMapService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 思维导图 Controller
 *
 * /api/mindmap/generate  必须登录（生成 + 入库）
 * /api/mindmap/list      必须登录（看自己历史）
 * /api/mindmap/{id}      必须登录（看自己某个导图）
 * /api/mindmap/{id}/tree 必须登录（拿树形结构）
 */
@Slf4j
@RestController
@RequestMapping("/api/mindmap")
@RequiredArgsConstructor
public class MindMapController {

    private final MindMapService mindMapService;
    private final JwtUtil jwtUtil;

    /**
     * 生成导图并入库
     */
    @PostMapping("/generate")
    public Result<MindMap> generate(@RequestBody MindMapGenerateRequest request, HttpServletRequest httpReq) {
        if (request.getTopic() == null || request.getTopic().isBlank()) {
            return Result.error(400, "topic 不能为空");
        }
        Long userId = requireUserId(httpReq);
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        try {
            MindMap saved = mindMapService.generateAndSave(userId, request.getTopic(), request.getContext());
            return Result.success(saved);
        } catch (RuntimeException e) {
            log.error("生成导图失败", e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 列出当前用户的所有导图
     */
    @GetMapping("/list")
    public Result<List<MindMap>> list(HttpServletRequest httpReq) {
        Long userId = requireUserId(httpReq);
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        return Result.success(mindMapService.listByUser(userId));
    }

    /**
     * 获取单个导图（含 tree_json）
     */
    @GetMapping("/{id}")
    public Result<MindMap> getById(@PathVariable Long id, HttpServletRequest httpReq) {
        Long userId = requireUserId(httpReq);
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        try {
            return Result.success(mindMapService.getById(userId, id));
        } catch (RuntimeException e) {
            return Result.error(404, e.getMessage());
        }
    }

    /**
     * 获取导图的树形 JSON（直接给前端渲染用）
     */
    @GetMapping("/{id}/tree")
    public Result<MindMapNode> getTree(@PathVariable Long id, HttpServletRequest httpReq) {
        Long userId = requireUserId(httpReq);
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        try {
            MindMap map = mindMapService.getById(userId, id);
            return Result.success(mindMapService.parseTreeJson(map.getTreeJson()));
        } catch (RuntimeException e) {
            return Result.error(404, e.getMessage());
        }
    }

    // ===== 辅助：从 Cookie 取 userId =====
    private Long requireUserId(HttpServletRequest request) {
        String token = getToken(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            return null;
        }
        return jwtUtil.getUserId(token);
    }

    private String getToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}