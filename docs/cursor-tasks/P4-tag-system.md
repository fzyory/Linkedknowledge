# P4 — 标签系统重构(`#tag` 解析)

## 1. 目标

复用现有 `Tag` 实体,但加上**内容里写 `#tag` 自动建标签**的能力。类似 Obsidian 的标签面板。

## 2. 验收标准

- [ ] 节点保存时,后端扫描 `content` 里的 `#xxx` 语法,自动建 `Tag`(如果不存在)+ 关联到节点
- [ ] 节点更新时,**重算**所有 tag 关联(基于当前 content,而不是累积)
- [ ] 节点删除时,只删 node_tag 中间表关联,**不删 Tag 本身**(其他节点可能还用)
- [ ] 标签名规范:小写字母数字 + 中划线下划线,最长 50,不允许以数字开头
- [ ] 前端节点详情页加"标签"区域,展示当前节点的所有 tag,可点击跳转 `/tags/{tagName}`
- [ ] 前端加 `/tags/{tagName}` 路由,展示所有用这个 tag 的节点列表
- [ ] 编辑节点时 `#tag` 高亮显示

## 3. 当前代码现状

- `Tag.java` 已存在,字段:`id`, `name`(unique)
- `KnowledgeNode.tags` 已是 `@ManyToMany` 关联到 `Tag`,中间表 `node_tag`
- 已有 `TagRepository` 吗?Cursor 自己定位(`src/main/java/com/LinkedKnowledge/repository/`)
- 前端:节点详情/编辑页已有(P1 阶段已动过)

## 4. 后端改动

### 4.1 新建工具类:`TagParser.java`

位置: `src/main/java/com/LinkedKnowledge/common/TagParser.java`

```java
package com.LinkedKnowledge.common;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析 #tag 语法
 *
 * 规则:
 *   #java           → 标签 "java"
 *   #Spring-Boot    → 标签 "spring-boot"
 *   #后端开发        → 标签 "后端开发"
 *
 * 不解析:
 *   #123            (不允许以数字开头)
 *   ##              (空标签)
 *   #tag with space (空格分隔,不解析第二个)
 */
public class TagParser {
    private static final Pattern PATTERN = Pattern.compile("#([a-zA-Z\\u4e00-\\u9fa5][a-zA-Z0-9_\\-\\u4e00-\\u9fa5]{0,49})");

    public static List<String> extract(String content) {
        if (content == null || content.isBlank()) return List.of();
        Set<String> result = new LinkedHashSet<>();
        Matcher m = PATTERN.matcher(content);
        while (m.find()) {
            String tag = m.group(1).toLowerCase();
            result.add(tag);
        }
        return new ArrayList<>(result);
    }
}
```

### 4.2 检查并新建:`TagRepository.java`

如果不存在,新建:

```java
package com.LinkedKnowledge.repository;

import com.LinkedKnowledge.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);
}
```

### 4.3 改 `KnowledgeNodeServiceImpl.updateNode(...)`

加一段:

```java
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
```

在 `createNode / updateNode` 末尾调 `rebuildTags(node)`。

## 5. 前端改动

### 5.1 `NodeDetail.vue` 加标签区

```vue
<section class="tags glass-panel">
  <h3>标签</h3>
  <router-link
    v-for="tag in node.tags"
    :key="tag.id"
    :to="`/tags/${tag.name}`"
    class="tag-chip"
  >#{{ tag.name }}</router-link>
</section>

<style scoped>
.tag-chip {
  display: inline-block;
  padding: 4px 12px;
  margin: 4px;
  background: rgba(168, 85, 247, 0.15);
  border: 1px solid rgba(168, 85, 247, 0.5);
  color: #A855F7;
  border-radius: 12px;
  font-size: 13px;
  text-decoration: none;
  transition: all 0.2s;
}
.tag-chip:hover {
  background: rgba(168, 85, 247, 0.3);
  box-shadow: 0 0 12px rgba(168, 85, 247, 0.5);
}
</style>
```

### 5.2 新建路由:`src/views/TagView.vue`

```vue
<template>
  <div class="tag-view">
    <h1 class="page-title">#{{ tagName }}</h1>
    <div v-if="nodes.length === 0" class="empty">没有节点使用这个标签</div>
    <div v-else class="node-list">
      <router-link
        v-for="n in nodes"
        :key="n.id"
        :to="`/node/${n.id}`"
        class="node-card glass-panel"
      >
        <h3>{{ n.title }}</h3>
        <p>{{ n.content?.substring(0, 100) }}</p>
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const tagName = route.params.name
const nodes = ref([])
const token = localStorage.getItem('jwt')
const headers = { Authorization: `Bearer ${token}` }

onMounted(async () => {
  const res = await axios.get(`/api/tags/${tagName}/nodes`, { headers })
  nodes.value = res.data
})
</script>

<style scoped>
.node-list { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; padding: 16px; }
.node-card { padding: 16px; color: #E0E7FF; }
.node-card h3 { color: #00E5FF; margin: 0 0 8px; }
</style>
```

### 5.3 加后端端点

`TagController.java`:

```java
@GetMapping("/{name}/nodes")
public List<KnowledgeNode> getNodesByTag(@PathVariable String name) {
    Tag tag = tagRepository.findByName(name).orElseThrow();
    // 用 @ManyToMany 的反向查
    return new ArrayList<>(tag.getNodes());
}
```

## 6. 测试要求

1. 创建节点 A,内容 "今天学了 #Java 多线程,准备做 #Spring-Boot 项目"
2. 保存后,数据库:
   - `tag` 表有 "java" 和 "spring-boot" 两条
   - `node_tag` 中间表有 A → "java" 和 A → "spring-boot" 关联
3. 编辑 A 内容,把 `#Java` 删掉,保存 → `node_tag` 中 A → "java" 关联消失,"spring-boot" 仍保留
4. 前端 `/tags/java` 应只显示其他含 `#Java` 的节点
5. `#123abc` 这种以数字开头的应**不被识别**为 tag

## 7. 不做的事

- **不**做"标签云"(后期 UI 增强)
- **不**做"标签自动建议"(和 AI 联想边重复,本期跳过)
- **不**改 Tag 表的字段结构
- **不**支持 `#中文-带连字符`(其实支持,只是说明)
