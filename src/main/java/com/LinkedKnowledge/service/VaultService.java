package com.LinkedKnowledge.service;

import com.LinkedKnowledge.dto.GraphData;
import com.LinkedKnowledge.entity.Attachment;
import com.LinkedKnowledge.entity.Bookmark;
import com.LinkedKnowledge.entity.EdgeStatus;
import com.LinkedKnowledge.entity.KnowledgeNode;
import com.LinkedKnowledge.entity.NodeEdge;
import com.LinkedKnowledge.entity.NoteTemplate;
import com.LinkedKnowledge.entity.NoteVersion;
import com.LinkedKnowledge.entity.VaultSettings;
import com.LinkedKnowledge.repository.AttachmentRepository;
import com.LinkedKnowledge.repository.BookmarkRepository;
import com.LinkedKnowledge.repository.KnowledgeNodeRepository;
import com.LinkedKnowledge.repository.NodeEdgeRepository;
import com.LinkedKnowledge.repository.NoteTemplateRepository;
import com.LinkedKnowledge.repository.NoteVersionRepository;
import com.LinkedKnowledge.repository.VaultSettingsRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VaultService {

    private final KnowledgeNodeRepository nodeRepository;
    private final KnowledgeNodeService nodeService;
    private final BookmarkRepository bookmarkRepository;
    private final NoteTemplateRepository templateRepository;
    private final NoteVersionRepository versionRepository;
    private final VaultSettingsRepository settingsRepository;
    private final AttachmentRepository attachmentRepository;
    private final NodeEdgeRepository edgeRepository;

    public List<KnowledgeNode> search(Long userId, String q) {
        if (q == null || q.isBlank()) return nodeRepository.findByUserIdWithTags(userId);
        return nodeRepository.searchVault(userId, q.trim());
    }

    @Transactional
    public KnowledgeNode daily(Long userId, String format) {
        String title = LocalDate.now().format(DateTimeFormatter.ofPattern(
                (format == null || format.isBlank()) ? "yyyy-MM-dd" : format));
        return nodeRepository.findFirstByTitleAndUserId(title, userId).orElseGet(() -> {
            KnowledgeNode n = new KnowledgeNode();
            n.setUserId(userId);
            n.setTitle(title);
            n.setContent("");
            n.setFolderPath("Daily");
            return nodeService.createNode(n);
        });
    }

    @Transactional
    public KnowledgeNode uniqueNote(Long userId, String body) {
        String title = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        KnowledgeNode n = new KnowledgeNode();
        n.setUserId(userId);
        n.setTitle(title);
        n.setContent(body == null ? "" : body);
        n.setFolderPath("Zettelkasten");
        return nodeService.createNode(n);
    }

    @Transactional
    public KnowledgeNode extract(Long userId, Long sourceId, String title, String excerpt) {
        nodeService.assertOwned(sourceId, userId);
        KnowledgeNode created = new KnowledgeNode();
        created.setUserId(userId);
        created.setTitle(title);
        created.setContent(excerpt == null ? "" : excerpt);
        KnowledgeNode saved = nodeService.createNode(created);
        KnowledgeNode source = nodeService.getNodeById(sourceId, userId);
        if (excerpt != null && source.getContent() != null && source.getContent().contains(excerpt)) {
            source.setContent(source.getContent().replace(excerpt, "[[" + saved.getTitle() + "]]"));
            nodeService.updateNode(sourceId, source, userId);
        }
        return saved;
    }

    public List<Bookmark> bookmarks(Long userId) {
        return bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public Bookmark toggleBookmark(Long userId, Long nodeId) {
        nodeService.assertOwned(nodeId, userId);
        return bookmarkRepository.findByUserIdAndNodeId(userId, nodeId)
                .map(b -> {
                    bookmarkRepository.delete(b);
                    return b;
                })
                .orElseGet(() -> {
                    KnowledgeNode n = nodeService.getNodeById(nodeId, userId);
                    Bookmark b = new Bookmark();
                    b.setUserId(userId);
                    b.setNodeId(nodeId);
                    b.setTitle(n.getTitle());
                    return bookmarkRepository.save(b);
                });
    }

    public List<NoteTemplate> templates(Long userId) {
        return templateRepository.findByUserIdOrderByNameAsc(userId);
    }

    @Transactional
    public NoteTemplate saveTemplate(Long userId, NoteTemplate incoming) {
        if (incoming.getId() != null) {
            NoteTemplate existing = templateRepository.findByIdAndUserId(incoming.getId(), userId)
                    .orElseThrow(() -> new RuntimeException("模板不存在"));
            existing.setName(incoming.getName());
            existing.setContent(incoming.getContent());
            return templateRepository.save(existing);
        }
        incoming.setId(null);
        incoming.setUserId(userId);
        return templateRepository.save(incoming);
    }

    @Transactional
    public void deleteTemplate(Long userId, Long id) {
        NoteTemplate t = templateRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("模板不存在"));
        templateRepository.delete(t);
    }

    public List<NoteVersion> versions(Long userId, Long nodeId) {
        nodeService.assertOwned(nodeId, userId);
        return versionRepository.findByUserIdAndNodeIdOrderByCreatedAtDesc(userId, nodeId);
    }

    @Transactional
    public KnowledgeNode restore(Long userId, Long versionId) {
        NoteVersion v = versionRepository.findByIdAndUserId(versionId, userId)
                .orElseThrow(() -> new RuntimeException("版本不存在"));
        KnowledgeNode node = nodeService.getNodeById(v.getNodeId(), userId);
        node.setTitle(v.getTitle());
        node.setContent(v.getContent());
        return nodeService.updateNode(node.getId(), node, userId);
    }

    public VaultSettings settings(Long userId) {
        return settingsRepository.findByUserId(userId).orElseGet(() -> {
            VaultSettings s = new VaultSettings();
            s.setUserId(userId);
            s.setSettingsJson(defaultSettings());
            s.setWorkspaceJson("{}");
            return settingsRepository.save(s);
        });
    }

    @Transactional
    public VaultSettings saveSettings(Long userId, String settingsJson, String workspaceJson) {
        VaultSettings s = settings(userId);
        if (settingsJson != null) s.setSettingsJson(settingsJson);
        if (workspaceJson != null) s.setWorkspaceJson(workspaceJson);
        return settingsRepository.save(s);
    }

    public AttachmentMeta saveAttachment(Long userId, Long nodeId, String filename, String contentType, String base64) {
        if (nodeId != null) nodeService.assertOwned(nodeId, userId);
        Attachment a = new Attachment();
        a.setUserId(userId);
        a.setNodeId(nodeId);
        a.setFilename(filename);
        a.setContentType(contentType);
        a.setData(Base64.getDecoder().decode(base64.contains(",") ? base64.substring(base64.indexOf(',') + 1) : base64));
        a = attachmentRepository.save(a);
        return meta(a);
    }

    public Attachment getAttachment(Long userId, Long id) {
        return attachmentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("附件不存在"));
    }

    public List<AttachmentMeta> listAttachments(Long userId) {
        return attachmentRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::meta).collect(Collectors.toList());
    }

    public GraphData localGraph(Long userId, Long nodeId) {
        nodeService.assertOwned(nodeId, userId);
        Set<Long> ids = new HashSet<>();
        ids.add(nodeId);
        List<NodeEdge> related = new ArrayList<>();
        related.addAll(edgeRepository.findBySourceNodeIdAndEdgeStatus(nodeId, EdgeStatus.ACCEPTED));
        related.addAll(edgeRepository.findByTargetNodeIdAndEdgeStatus(nodeId, EdgeStatus.ACCEPTED));
        for (NodeEdge e : related) {
            ids.add(e.getSourceNodeId());
            ids.add(e.getTargetNodeId());
        }
        List<KnowledgeNode> nodes = nodeRepository.findByUserId(userId).stream()
                .filter(n -> ids.contains(n.getId()))
                .collect(Collectors.toList());
        GraphData data = new GraphData();
        data.setNodes(nodes.stream().map(n -> {
            GraphData.Node gn = new GraphData.Node();
            gn.setId(n.getId());
            gn.setTitle(n.getTitle());
            gn.setType(n.getType() == null ? null : n.getType().name());
            return gn;
        }).collect(Collectors.toList()));
        data.setEdges(related.stream().map(e -> {
            GraphData.Edge ge = new GraphData.Edge();
            ge.setSource(e.getSourceNodeId());
            ge.setTarget(e.getTargetNodeId());
            ge.setType(e.getEdgeType() == null ? null : e.getEdgeType().name());
            return ge;
        }).collect(Collectors.toList()));
        return data;
    }

    public Map<String, Object> exportVault(Long userId) {
        Map<String, Object> vault = new LinkedHashMap<>();
        vault.put("exportedAt", LocalDateTime.now().toString());
        vault.put("notes", nodeRepository.findByUserIdWithTags(userId));
        vault.put("templates", templateRepository.findByUserIdOrderByNameAsc(userId));
        vault.put("bookmarks", bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId));
        vault.put("settings", settings(userId));
        return vault;
    }

    @Transactional
    public int importNotes(Long userId, List<Map<String, Object>> notes) {
        int count = 0;
        if (notes == null) return 0;
        for (Map<String, Object> raw : notes) {
            KnowledgeNode n = new KnowledgeNode();
            n.setUserId(userId);
            n.setTitle(String.valueOf(raw.getOrDefault("title", "未命名")));
            n.setContent(raw.get("content") == null ? "" : String.valueOf(raw.get("content")));
            n.setFolderPath(raw.get("folderPath") == null ? "" : String.valueOf(raw.get("folderPath")));
            n.setAliases(raw.get("aliases") == null ? "" : String.valueOf(raw.get("aliases")));
            nodeService.createNode(n);
            count++;
        }
        return count;
    }

    private AttachmentMeta meta(Attachment a) {
        AttachmentMeta m = new AttachmentMeta();
        m.setId(a.getId());
        m.setNodeId(a.getNodeId());
        m.setFilename(a.getFilename());
        m.setContentType(a.getContentType());
        m.setUrl("/api/vault/attachments/" + a.getId() + "/raw");
        return m;
    }

    private String defaultSettings() {
        return "{\"theme\":\"dark\",\"dailyFormat\":\"yyyy-MM-dd\",\"plugins\":{"
                + "\"fileExplorer\":true,\"search\":true,\"bookmarks\":true,\"graph\":true,"
                + "\"canvas\":true,\"dailyNotes\":true,\"templates\":true,\"commandPalette\":true,"
                + "\"outgoingLinks\":true,\"backlinks\":true,\"outline\":true,\"tags\":true,"
                + "\"pagePreview\":true,\"noteComposer\":true,\"uniqueNote\":true,\"wordCount\":true,"
                + "\"fileRecovery\":true,\"workspaces\":true,\"slashCommand\":true,\"slides\":true,"
                + "\"audioRecorder\":true,\"webViewer\":true,\"bases\":true,\"properties\":true,"
                + "\"publish\":true}}";
    }

    @Data
    public static class AttachmentMeta {
        private Long id;
        private Long nodeId;
        private String filename;
        private String contentType;
        private String url;
    }
}
