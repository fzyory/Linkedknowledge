package com.LinkedKnowledge.controller;

import com.LinkedKnowledge.common.AuthHelper;
import com.LinkedKnowledge.common.Result;
import com.LinkedKnowledge.dto.NodeSummary;
import com.LinkedKnowledge.entity.Tag;
import com.LinkedKnowledge.repository.KnowledgeNodeRepository;
import com.LinkedKnowledge.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagRepository tagRepository;
    private final KnowledgeNodeRepository nodeRepository;

    /** 当前用户用过的所有标签 */
    @GetMapping
    public Result<List<Tag>> listMyTags() {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        return Result.success(tagRepository.findUsedByUserId(userId));
    }

    /** 某标签下当前用户的节点（slim: id / title / content） */
    @GetMapping("/{name}/nodes")
    public Result<List<NodeSummary>> getNodesByTag(@PathVariable String name) {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        String tagName = name == null ? "" : name.toLowerCase();
        List<NodeSummary> nodes = nodeRepository.findByTagNameAndUserId(tagName, userId).stream()
                .map(n -> new NodeSummary(n.getId(), n.getTitle(), n.getContent()))
                .collect(Collectors.toList());
        return Result.success(nodes);
    }
}
