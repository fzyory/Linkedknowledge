package com.LinkedKnowledge.controller;

import com.LinkedKnowledge.common.AuthHelper;
import com.LinkedKnowledge.common.Result;
import com.LinkedKnowledge.dto.GraphData;
import com.LinkedKnowledge.entity.Attachment;
import com.LinkedKnowledge.entity.Bookmark;
import com.LinkedKnowledge.entity.KnowledgeNode;
import com.LinkedKnowledge.entity.NoteTemplate;
import com.LinkedKnowledge.entity.NoteVersion;
import com.LinkedKnowledge.entity.VaultSettings;
import com.LinkedKnowledge.service.VaultService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vault")
@RequiredArgsConstructor
public class VaultController {

    private final VaultService vaultService;

    @GetMapping("/search")
    public Result<List<KnowledgeNode>> search(@RequestParam(defaultValue = "") String q) {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(vaultService.search(userId, q));
    }

    @PostMapping("/daily")
    public Result<KnowledgeNode> daily(@RequestBody(required = false) DailyRequest body) {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        String format = body == null ? null : body.getFormat();
        return Result.success(vaultService.daily(userId, format));
    }

    @PostMapping("/unique")
    public Result<KnowledgeNode> unique(@RequestBody(required = false) UniqueRequest body) {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(vaultService.uniqueNote(userId, body == null ? null : body.getContent()));
    }

    @PostMapping("/extract")
    public Result<KnowledgeNode> extract(@RequestBody ExtractRequest body) {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(vaultService.extract(userId, body.getSourceNodeId(), body.getTitle(), body.getExcerpt()));
    }

    @GetMapping("/bookmarks")
    public Result<List<Bookmark>> bookmarks() {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(vaultService.bookmarks(userId));
    }

    @PostMapping("/bookmarks/{nodeId}")
    public Result<Bookmark> toggleBookmark(@PathVariable Long nodeId) {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(vaultService.toggleBookmark(userId, nodeId));
    }

    @GetMapping("/templates")
    public Result<List<NoteTemplate>> templates() {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(vaultService.templates(userId));
    }

    @PostMapping("/templates")
    public Result<NoteTemplate> saveTemplate(@RequestBody NoteTemplate template) {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(vaultService.saveTemplate(userId, template));
    }

    @DeleteMapping("/templates/{id}")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        vaultService.deleteTemplate(userId, id);
        return Result.success();
    }

    @GetMapping("/versions/{nodeId}")
    public Result<List<NoteVersion>> versions(@PathVariable Long nodeId) {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(vaultService.versions(userId, nodeId));
    }

    @PostMapping("/versions/{id}/restore")
    public Result<KnowledgeNode> restore(@PathVariable Long id) {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(vaultService.restore(userId, id));
    }

    @GetMapping("/settings")
    public Result<VaultSettings> settings() {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(vaultService.settings(userId));
    }

    @PutMapping("/settings")
    public Result<VaultSettings> saveSettings(@RequestBody VaultSettings body) {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(vaultService.saveSettings(userId, body.getSettingsJson(), body.getWorkspaceJson()));
    }

    @GetMapping("/graph/local/{nodeId}")
    public Result<GraphData> localGraph(@PathVariable Long nodeId) {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(vaultService.localGraph(userId, nodeId));
    }

    @GetMapping("/export")
    public Result<Map<String, Object>> exportVault() {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(vaultService.exportVault(userId));
    }

    @PostMapping("/import")
    public Result<Integer> importVault(@RequestBody ImportRequest body) {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(vaultService.importNotes(userId, body.getNotes()));
    }

    @GetMapping("/attachments")
    public Result<List<VaultService.AttachmentMeta>> attachments() {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(vaultService.listAttachments(userId));
    }

    @PostMapping("/attachments")
    public Result<VaultService.AttachmentMeta> upload(@RequestBody UploadRequest body) {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(vaultService.saveAttachment(userId, body.getNodeId(), body.getFilename(), body.getContentType(), body.getBase64()));
    }

    @GetMapping("/attachments/{id}/raw")
    public ResponseEntity<byte[]> raw(@PathVariable Long id) {
        Long userId = AuthHelper.currentUserId();
        if (userId == null) return ResponseEntity.status(401).build();
        Attachment a = vaultService.getAttachment(userId, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + a.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(a.getContentType() == null ? "application/octet-stream" : a.getContentType()))
                .body(a.getData());
    }

    @Data
    public static class DailyRequest { private String format; }
    @Data
    public static class UniqueRequest { private String content; }
    @Data
    public static class ExtractRequest {
        private Long sourceNodeId;
        private String title;
        private String excerpt;
    }
    @Data
    public static class UploadRequest {
        private Long nodeId;
        private String filename;
        private String contentType;
        private String base64;
    }
    @Data
    public static class ImportRequest {
        private List<Map<String, Object>> notes;
    }
}
