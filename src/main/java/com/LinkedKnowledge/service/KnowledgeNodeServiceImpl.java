package com.LinkedKnowledge.service;

import com.LinkedKnowledge.common.TagParser;
import com.LinkedKnowledge.common.WikiLinkParser;
import com.LinkedKnowledge.entity.EdgeStatus;
import com.LinkedKnowledge.entity.EdgeType;
import com.LinkedKnowledge.entity.KnowledgeNode;
import com.LinkedKnowledge.entity.NodeEdge;
import com.LinkedKnowledge.entity.NodeType;
import com.LinkedKnowledge.entity.NoteVersion;
import com.LinkedKnowledge.entity.Tag;
import com.LinkedKnowledge.repository.AiEdgeSuggestionRepository;
import com.LinkedKnowledge.repository.AttachmentRepository;
import com.LinkedKnowledge.repository.BookmarkRepository;
import com.LinkedKnowledge.repository.KnowledgeNodeRepository;
import com.LinkedKnowledge.repository.NodeEdgeRepository;
import com.LinkedKnowledge.repository.NoteVersionRepository;
import com.LinkedKnowledge.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeNodeServiceImpl implements KnowledgeNodeService {

    private final KnowledgeNodeRepository nodeRepository;
    private final NodeEdgeRepository edgeRepository;
    private final TagRepository tagRepository;
    private final EdgeSuggestionService edgeSuggestionService;
    private final AiEdgeSuggestionRepository suggestionRepository;
    private final NoteVersionRepository versionRepository;
    private final BookmarkRepository bookmarkRepository;
    private final AttachmentRepository attachmentRepository;
    private final MemoryService memoryService;

    @Override
    @Transactional
    public KnowledgeNode createNode(KnowledgeNode node) {
        node.setId(null);
        if (node.getFolderPath() == null) node.setFolderPath("");
        if (node.getAliases() == null) node.setAliases("");
        if (node.getPropertiesJson() == null) node.setPropertiesJson("");
        if (node.getStarred() == null) node.setStarred(false);
        KnowledgeNode saved = nodeRepository.save(node);
        rebuildParsedLinks(saved);
        rebuildTags(saved);
        saved = nodeRepository.save(saved);
        triggerSuggestion(saved.getId());
        triggerRetain(saved.getId());
        return nodeRepository.findByIdWithTags(saved.getId()).orElse(saved);
    }

    @Override
    @Transactional
    public KnowledgeNode updateNode(Long id, KnowledgeNode updatedNode, Long userId) {
        KnowledgeNode existing = requireOwned(id, userId);
        snapshot(existing);
        String oldTitle = existing.getTitle();
        existing.setTitle(updatedNode.getTitle());
        existing.setContent(updatedNode.getContent());
        existing.setStatus(updatedNode.getStatus());
        existing.setType(updatedNode.getType());
        existing.setDifficultyLevel(updatedNode.getDifficultyLevel());
        if (updatedNode.getFolderPath() != null) existing.setFolderPath(updatedNode.getFolderPath());
        if (updatedNode.getAliases() != null) existing.setAliases(updatedNode.getAliases());
        if (updatedNode.getPropertiesJson() != null) existing.setPropertiesJson(updatedNode.getPropertiesJson());
        if (updatedNode.getStarred() != null) existing.setStarred(updatedNode.getStarred());
        nodeRepository.save(existing);
        rebuildParsedLinks(existing);
        rebuildTags(existing);
        existing = nodeRepository.save(existing);
        if (oldTitle != null && updatedNode.getTitle() != null && !oldTitle.equals(updatedNode.getTitle())) {
            cascadeRenameLinks(userId, oldTitle, updatedNode.getTitle(), existing.getId());
        }
        triggerSuggestion(existing.getId());
        triggerRetain(existing.getId());
        return nodeRepository.findByIdWithTags(existing.getId()).orElse(existing);
    }

    @Override
    @Transactional
    public void deleteNode(Long id, Long userId) {
        requireOwned(id, userId);
        edgeRepository.deleteAllByNodeId(id);
        suggestionRepository.deleteBySourceNodeIdOrTargetNodeId(id, id);
        versionRepository.deleteByNodeId(id);
        bookmarkRepository.deleteByNodeId(id);
        attachmentRepository.deleteByNodeId(id);
        memoryService.deleteFactsForNode(id);
        nodeRepository.deleteById(id);
    }

    @Override
    public KnowledgeNode getNodeById(Long id, Long userId) {
        return requireOwned(id, userId);
    }

    @Override
    public List<KnowledgeNode> getNodesByType(NodeType type, Long userId) {
        return nodeRepository.findByTypeAndUserId(type, userId);
    }

    @Override
    public List<KnowledgeNode> getAllNodes(Long userId) {
        return nodeRepository.findByUserIdWithTags(userId);
    }

    @Override
    public List<KnowledgeNode> getRootNodes(Long userId) {
        return nodeRepository.findByParentIsNullAndUserId(userId);
    }

    @Override
    public List<KnowledgeNode> searchByTitle(String keyword, Long userId) {
        return nodeRepository.findByTitleContainingAndUserId(keyword, userId);
    }

    @Override
    public void assertOwned(Long nodeId, Long userId) {
        requireOwned(nodeId, userId);
    }

    private KnowledgeNode requireOwned(Long id, Long userId) {
        KnowledgeNode node = nodeRepository.findByIdWithTags(id)
                .orElseThrow(() -> new RuntimeException("节点不存在"));
        if (node.getUserId() == null || !node.getUserId().equals(userId)) {
            throw new RuntimeException("节点不存在");
        }
        return node;
    }

    /**
     * 扫描 content 里的 [[xxx]]，先删旧 PARSED_LINK 再按标题建边。
     * 目标节点不存在则 warn 并跳过，不建边、不建节点。
     */
    private void rebuildParsedLinks(KnowledgeNode node) {
        List<String> titles = WikiLinkParser.extract(node.getContent());
        edgeRepository.deleteParsedLinksBySource(node.getId());
        Set<String> uniqueTitles = new LinkedHashSet<>(titles);
        for (String title : uniqueTitles) {
            Optional<KnowledgeNode> target = nodeRepository.findFirstByTitleAndUserId(title, node.getUserId());
            if (target.isEmpty()) {
                log.warn("Wiki 链接目标不存在，跳过: title='{}' sourceNodeId={}", title, node.getId());
                continue;
            }
            KnowledgeNode t = target.get();
            if (t.getId().equals(node.getId())) {
                continue;
            }
            NodeEdge edge = new NodeEdge();
            edge.setSourceNodeId(node.getId());
            edge.setTargetNodeId(t.getId());
            edge.setEdgeType(EdgeType.PARSED_LINK);
            edge.setEdgeStatus(EdgeStatus.ACCEPTED);
            edge.setAcceptedAt(LocalDateTime.now());
            edge.setSourceText("[[" + title + "]]");
            edgeRepository.save(edge);
        }
    }

    /**
     * 按当前 content 重算 #tag 关联。不删 Tag 行本身。
     */
    private void rebuildTags(KnowledgeNode node) {
        if (node.getContent() == null) return;
        List<String> tagNames = TagParser.extract(node.getContent());
        Set<Tag> tags = new HashSet<>();
        for (String name : tagNames) {
            Tag tag = tagRepository.findByName(name)
                    .orElseGet(() -> {
                        Tag t = new Tag();
                        t.setName(name);
                        return tagRepository.save(t);
                    });
            tags.add(tag);
        }
        node.setTags(tags);
    }

    private void snapshot(KnowledgeNode node) {
        NoteVersion v = new NoteVersion();
        v.setUserId(node.getUserId());
        v.setNodeId(node.getId());
        v.setTitle(node.getTitle());
        v.setContent(node.getContent());
        versionRepository.save(v);
        List<NoteVersion> all = versionRepository.findByUserIdAndNodeIdOrderByCreatedAtDesc(node.getUserId(), node.getId());
        if (all.size() > 30) {
            for (int i = 30; i < all.size(); i++) {
                versionRepository.delete(all.get(i));
            }
        }
    }

    private void cascadeRenameLinks(Long userId, String oldTitle, String newTitle, Long skipId) {
        String needle = "[[" + oldTitle;
        List<KnowledgeNode> all = nodeRepository.findByUserId(userId);
        for (KnowledgeNode n : all) {
            if (n.getId().equals(skipId) || n.getContent() == null) continue;
            if (!n.getContent().contains(needle)) continue;
            String updated = n.getContent()
                    .replace("[[" + oldTitle + "]]", "[[" + newTitle + "]]")
                    .replace("[[" + oldTitle + "|", "[[" + newTitle + "|")
                    .replace("[[" + oldTitle + "#", "[[" + newTitle + "#")
                    .replace("![[" + oldTitle + "]]", "![[" + newTitle + "]]")
                    .replace("![[" + oldTitle + "#", "![[" + newTitle + "#");
            n.setContent(updated);
            nodeRepository.save(n);
            rebuildParsedLinks(n);
        }
    }

    private void triggerSuggestion(Long nodeId) {
        afterCommit(() -> edgeSuggestionService.suggestRelated(nodeId));
    }

    private void triggerRetain(Long nodeId) {
        afterCommit(() -> memoryService.retainNoteAsync(nodeId));
    }

    private void afterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }
}
