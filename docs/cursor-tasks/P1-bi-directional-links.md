# P1 — 反向链接 + 双向链接解析(`[[xxx]]`)

## 1. 目标

实现 Obsidian 最核心的两个功能:

1. **双向链接语法** — 用户在节点内容里写 `[[Java 多线程]]`,系统自动建一条边
2. **反向链接面板** — 节点详情页显示"哪些节点引用了我"

## 2. 验收标准

- [ ] 节点保存时,后端扫描 `content` 里的 `[[xxx]]` 语法,自动建 `NodeEdge(edgeType=PARSED_LINK, status=ACCEPTED)`
- [ ] 节点**更新**时,先删除旧的所有 `PARSED_LINK` 边,再建新的(否则会有孤儿边)
- [ ] 节点**删除**时,删除所有涉及该节点的边(source 或 target)
- [ ] 解析不存在的节点名时,日志 warn 但不报错(允许先写链接,后建节点)
- [ ] 前端节点详情页加"反向链接"区域,列出"X 个节点引用了我" + 节点列表
- [ ] 前端节点详情页加"正向链接"区域,列出"我引用了 X 个节点"
- [ ] 前端编辑节点时,`[[xxx]]` 高亮显示(可暂用 textarea + 正则匹配,后期换编辑器)
- [ ] 后端 `/api/edges/target/{id}` `/api/edges/source/{id}` 返回 DTO(带节点标题,不要只返回 nodeId)

## 3. 当前代码现状

- 后端: `C:\Users\g\Desktop\linkedknowledge`
- `KnowledgeNodeServiceImpl` 在 `src/main/java/com/LinkedKnowledge/service/KnowledgeNodeServiceImpl.java`,已有 `register / login / findById / deleteAccount / changePassword` 等方法,**但注意这些是 UserService 的方法**,找真正的 `KnowledgeNodeServiceImpl`
- `KnowledgeNodeController` 在 `src/main/java/com/LinkedKnowledge/controller/KnowledgeNodeController.java`
- 前端: `C:\Users\g\Desktop\mindmap-ui`
- 节点编辑页 / 详情页位置: `src/views/` 目录(具体文件名 Cursor 自己定位,大概是 `NodeEditor.vue` 或 `NodeDetail.vue`)

## 4. 后端改动

### 4.1 新建工具类:`WikiLinkParser.java`

位置: `src/main/java/com/LinkedKnowledge/common/WikiLinkParser.java`

```java
package com.LinkedKnowledge.common;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析 [[xxx]] 语法
 *
 * 支持:
 *   [[节点标题]]                → 单链接
 *   [[节点标题|别名]]            → 显示别名,链接到"节点标题"
 *   [[节点标题#块id]]            → 块引用(本期暂存标题,不解析块,放 P6)
 */
public class WikiLinkParser {
    // 匹配 [[xxx]],xxx 里不能有 [] 和换行
    private static final Pattern PATTERN = Pattern.compile("\\[\\[([^\\[\\]\\n]+?)\\]\\]");

    public static List<String> extract(String content) {
        if (content == null || content.isBlank()) return List.of();
        List<String> result = new ArrayList<>();
        Matcher m = PATTERN.matcher(content);
        while (m.find()) {
            String raw = m.group(1).trim();
            // 处理 |别名
            int pipe = raw.indexOf('|');
            if (pipe >= 0) raw = raw.substring(0, pipe).trim();
            // 处理 #块引用(本期先存标题)
            int hash = raw.indexOf('#');
            if (hash >= 0) raw = raw.substring(0, hash).trim();
            if (!raw.isEmpty()) result.add(raw);
        }
        return result;
    }
}
```

### 4.2 改 `KnowledgeNodeServiceImpl`

在 `save / update` 节点时,扫描 `content` 调 `WikiLinkParser.extract`,然后:
1. 查 `KnowledgeNodeRepository.findByTitle(title)`,找到 target nodeId
2. 在 `NodeEdgeRepository` 里**先删旧**(`edgeType=PARSED_LINK AND source_node_id=thisNodeId`)
3. 再**批量插新**

具体逻辑 Cursor 实现,关键点:

- `createNode(...)` 方法:save 之后调 `rebuildParsedLinks(nodeId, content)`
- `updateNode(...)` 方法:save 之后调同样的 `rebuildParsedLinks(nodeId, content)`
- `deleteNode(nodeId)`:先删 node,再 `nodeEdgeRepository.deleteBySourceNodeIdOrTargetNodeId(...)`

### 4.3 Repository 加方法

**NodeEdgeRepository.java** 加:

```java
@Modifying
@Query("DELETE FROM NodeEdge e WHERE e.sourceNodeId = :nodeId AND e.edgeType = 'PARSED_LINK'")
void deleteParsedLinksBySource(@Param("nodeId") Long nodeId);

@Modifying
@Query("DELETE FROM NodeEdge e WHERE e.sourceNodeId = :nodeId OR e.targetNodeId = :nodeId")
void deleteAllByNodeId(@Param("nodeId") Long nodeId);
```

### 4.4 新建 DTO:`NodeEdgeWithTitle.java`

位置: `src/main/java/com/LinkedKnowledge/dto/NodeEdgeWithTitle.java`

```java
package com.LinkedKnowledge.dto;

import com.LinkedKnowledge.entity.EdgeType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NodeEdgeWithTitle {
    private Long edgeId;
    private Long sourceNodeId;
    private String sourceNodeTitle;
    private Long targetNodeId;
    private String targetNodeTitle;
    private EdgeType edgeType;
    private LocalDateTime createdAt;
}
```

### 4.5 新建 Service 转换:`NodeEdgeMapper.java`

把 `NodeEdge` + 节点标题批量转成 `NodeEdgeWithTitle`。Cursor 自己实现,提示:
- 用 `KnowledgeNodeRepository.findAllById(...)` 一次查所有 id
- 内存里组装 Map<id, title>
- 再 map 边

### 4.6 改 `NodeEdgeController` 的 bySource / byTarget

返回类型从 `List<NodeEdge>` 改成 `List<NodeEdgeWithTitle>`,内部调 mapper。

## 5. 前端改动

### 5.1 节点详情页

文件位置: `mindmap-ui/src/views/NodeDetail.vue` 或类似,Cursor 自己找。

新增两个区域:

```vue
<template>
  <div class="node-detail">
    <h1>{{ node.title }}</h1>
    <div class="content">{{ node.content }}</div>

    <!-- 反向链接 -->
    <section class="backlinks glass-panel">
      <h3>反向链接 ({{ backlinks.length }})</h3>
      <ul>
        <li v-for="edge in backlinks" :key="edge.edgeId">
          <router-link :to="`/node/${edge.sourceNodeId}`">
            {{ edge.sourceNodeTitle }}
          </router-link>
        </li>
      </ul>
    </section>

    <!-- 正向链接 -->
    <section class="forward-links glass-panel">
      <h3>正向链接 ({{ forwardLinks.length }})</h3>
      <ul>
        <li v-for="edge in forwardLinks" :key="edge.edgeId">
          <router-link :to="`/node/${edge.targetNodeId}`">
            {{ edge.targetNodeTitle }}
          </router-link>
        </li>
      </ul>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const node = ref({})
const backlinks = ref([])
const forwardLinks = ref([])

const nodeId = route.params.id
const token = localStorage.getItem('jwt')
const headers = { Authorization: `Bearer ${token}` }

onMounted(async () => {
  const [nodeRes, backRes, forwardRes] = await Promise.all([
    axios.get(`/api/knowledge-nodes/${nodeId}`, { headers }),
    axios.get(`/api/edges/target/${nodeId}`, { headers }),
    axios.get(`/api/edges/source/${nodeId}`, { headers }),
  ])
  node.value = nodeRes.data
  backlinks.value = backRes.data
  forwardLinks.value = forwardRes.data
})
</script>

<style scoped>
/* 液态科技风样式 — 用 P5 规范的 .glass-panel 类 */
.glass-panel {
  background: rgba(10, 14, 26, 0.6);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(0, 229, 255, 0.3);
  border-radius: 12px;
  padding: 16px;
  margin: 16px 0;
}
</style>
```

### 5.2 节点编辑页 — 临时高亮 `[[xxx]]`

本期先用 textarea + 正则匹配高亮(后期 P6 可换 Monaco editor):

```vue
<textarea
  v-model="content"
  @input="onContentInput"
  class="node-content"
/>

<div class="preview" v-html="highlightedContent" />
```

`highlightedContent` 把 `[[xxx]]` 替换成 `<a class="wikilink" href="/node/...">xxx</a>`。

## 6. 测试要求

1. 启动后端 + 前端
2. 创建节点 A,标题 "Java 多线程",内容写 "学习 [[JVM 内存模型]] 和 [[垃圾回收]]"
3. 保存后,数据库应自动有两条边:A → "JVM 内存模型"(标题找不到时 target_node_id 是 NULL? — **Cursor 这里要决策**)

> **Cursor 决策点**:`[[xxx]]` 指向的标题如果不存在,怎么办?
>
> 选项 1: 不建边,只在前端显示橙色虚线(警告"目标未建")
> 选项 2: 建一条 `target_node_id=NULL` 的边
> 选项 3: 直接建一个临时节点(自动建节点)
>
> **本项目建议选项 1**(本期不做),所以解析时找不到节点**直接跳过,不报错**

4. 创建节点 B,标题 "JVM 内存模型",内容里写 "[[Java 多线程]]"
5. 打开节点 A 详情页,应看到反向链接区有 B
6. 打开节点 B 详情页,应看到反向链接区有 A
7. 修改节点 A 内容,把 "[[垃圾回收]]" 删掉,保存 — 数据库里 A→垃圾回收 的边应该消失
8. 删除节点 A — 所有以 A 为 source 或 target 的边都应消失

## 7. 不做的事

- **不**实现块引用解析(`[[标题#块id]]` 的 # 后半部分本期跳过)
- **不**实现链接补全(用户输入 `[[` 时不弹出候选列表,后期 P6)
- **不**重构 user / jwt / SecurityConfig
- **不**改 KnowledgeNode.parent/children
- **不**做全文搜索(放 P4)
