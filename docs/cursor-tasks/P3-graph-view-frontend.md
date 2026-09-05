# P3 — 全局图谱视图(前端为主)

## 1. 目标

实现 Obsidian 风格的**全局图谱视图**——把用户所有节点 + 所有 ACCEPTED 边,以力导向图(Force-Directed Graph)的形式可视化。

技术选型:**D3.js + d3-force**(成熟,文档全,体积可控)。不用 Konva(它是 2D 画布,做力导向不顺手)。

## 2. 验收标准

- [ ] 路由 `/graph` 可访问,显示当前登录用户的所有节点和边
- [ ] 节点是圆形,可拖拽
- [ ] 节点 hover 显示标题 tooltip
- [ ] 边是带箭头直线,表示 source → target
- [ ] 物理引擎:节点互相排斥,边像弹簧一样拉近,稳定后停下
- [ ] 节点点击跳转到 `/node/{id}`
- [ ] 液态科技风样式:节点深空黑 + 青色描边,边是半透明青色,hover 时节点变亮
- [ ] 空状态:用户没有节点时显示"还没有节点,去创建一个吧"
- [ ] 节点数 > 100 时仍能流畅(后端限制一次最多返回 500 节点)

## 3. 当前代码现状

- 前端: `C:\Users\g\Desktop\mindmap-ui`
- 已有路由在 `src/router/` 目录
- 已有 `views/` 目录
- 后端 P0 已建 NodeEdgeRepository,有 `findAcceptedBySource / findAcceptedByTarget`

## 4. 后端改动

### 4.1 新建 Controller 端点:`GraphController.java`

位置: `src/main/java/com/LinkedKnowledge/controller/GraphController.java`

```java
package com.LinkedKnowledge.controller;

import com.LinkedKnowledge.dto.GraphData;
import com.LinkedKnowledge.entity.KnowledgeNode;
import com.LinkedKnowledge.entity.NodeEdge;
import com.LinkedKnowledge.repository.KnowledgeNodeRepository;
import com.LinkedKnowledge.repository.NodeEdgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/graph")
@RequiredArgsConstructor
public class GraphController {

    private final KnowledgeNodeRepository nodeRepository;
    private final NodeEdgeRepository edgeRepository;

    /**
     * 返回当前用户的所有节点 + 所有 ACCEPTED 边
     * 用于前端图谱可视化
     */
    @GetMapping
    public GraphData getMyGraph(@AuthenticationPrincipal UserDetails user) {
        // 从 SecurityContext 取 userId(具体怎么取 Cursor 自己适配现有 SecurityConfig)
        Long userId = extractUserId(user);

        List<KnowledgeNode> nodes = nodeRepository.findByUserId(userId);
        Set<Long> nodeIds = nodes.stream().map(KnowledgeNode::getId).collect(Collectors.toSet());

        // 取所有 ACCEPTED 边,只保留 source 和 target 都在 nodeIds 里的
        List<NodeEdge> edges = new ArrayList<>();
        for (KnowledgeNode n : nodes) {
            edges.addAll(edgeRepository.findAcceptedBySource(n.getId()));
        }
        edges = edges.stream()
                .filter(e -> nodeIds.contains(e.getSourceNodeId()) && nodeIds.contains(e.getTargetNodeId()))
                .distinct()
                .collect(Collectors.toList());

        GraphData data = new GraphData();
        data.setNodes(nodes.stream().map(n -> {
            GraphData.Node gn = new GraphData.Node();
            gn.setId(n.getId());
            gn.setTitle(n.getTitle());
            gn.setType(n.getType() == null ? null : n.getType().name());
            return gn;
        }).collect(Collectors.toList()));
        data.setEdges(edges.stream().map(e -> {
            GraphData.Edge ge = new GraphData.Edge();
            ge.setSource(e.getSourceNodeId());
            ge.setTarget(e.getTargetNodeId());
            ge.setType(e.getEdgeType().name());
            return ge;
        }).collect(Collectors.toList()));
        return data;
    }

    private Long extractUserId(UserDetails user) {
        // 适配现有 SecurityConfig,JWT 里 userId 怎么拿 Cursor 自己看
        // 提示:看现有 controller 怎么取 userId,比如 KnowledgeNodeController 怎么写的
        return 1L; // placeholder,Cursor 必须替换成真实逻辑
    }
}
```

### 4.2 新建 DTO:`GraphData.java`

```java
package com.LinkedKnowledge.dto;

import lombok.Data;
import java.util.List;

@Data
public class GraphData {
    private List<Node> nodes;
    private List<Edge> edges;

    @Data
    public static class Node {
        private Long id;
        private String title;
        private String type;
    }

    @Data
    public static class Edge {
        private Long source;
        private Long target;
        private String type;
    }
}
```

## 5. 前端改动

### 5.1 安装 D3

```bash
cd mindmap-ui
npm install d3
```

### 5.2 新建路由:`src/views/GraphView.vue`

```vue
<template>
  <div class="graph-view">
    <h1 class="page-title">知识图谱</h1>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="!data || data.nodes.length === 0" class="empty glass-panel">
      <p>还没有节点,去创建一个吧</p>
      <router-link to="/" class="btn-primary">返回首页</router-link>
    </div>
    <svg ref="svgRef" v-else class="graph-svg" />
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import * as d3 from 'd3'
import axios from 'axios'

const svgRef = ref(null)
const data = ref(null)
const loading = ref(true)

const token = localStorage.getItem('jwt')
const headers = { Authorization: `Bearer ${token}` }

onMounted(async () => {
  const res = await axios.get('/api/graph', { headers })
  data.value = res.data
  loading.value = false
  if (data.value.nodes.length > 0) renderGraph()
})

function renderGraph() {
  const svg = d3.select(svgRef.value)
  const width = svgRef.value.clientWidth
  const height = svgRef.value.clientHeight

  svg.selectAll('*').remove()

  // 液态科技风配色
  const COLOR_NODE = '#00E5FF'
  const COLOR_NODE_STROKE = '#0A0E1A'
  const COLOR_EDGE = 'rgba(0, 229, 255, 0.4)'
  const COLOR_NODE_HOVER = '#A855F7'

  // 力模拟
  const simulation = d3.forceSimulation(data.value.nodes)
      .force('link', d3.forceLink(data.value.edges).id(d => d.id).distance(120))
      .force('charge', d3.forceManyBody().strength(-300))
      .force('center', d3.forceCenter(width / 2, height / 2))
      .force('collide', d3.forceCollide(30))

  // 箭头
  svg.append('defs').append('marker')
      .attr('id', 'arrowhead')
      .attr('viewBox', '0 -5 10 10')
      .attr('refX', 20)
      .attr('refY', 0)
      .attr('markerWidth', 6)
      .attr('markerHeight', 6)
      .attr('orient', 'auto')
      .append('path')
      .attr('d', 'M0,-5L10,0L0,5')
      .attr('fill', COLOR_EDGE)

  const link = svg.append('g')
      .selectAll('line')
      .data(data.value.edges)
      .join('line')
      .attr('stroke', COLOR_EDGE)
      .attr('stroke-width', 1.5)
      .attr('marker-end', 'url(#arrowhead)')

  const node = svg.append('g')
      .selectAll('circle')
      .data(data.value.nodes)
      .join('circle')
      .attr('r', 12)
      .attr('fill', COLOR_NODE)
      .attr('stroke', COLOR_NODE_STROKE)
      .attr('stroke-width', 2)
      .style('cursor', 'pointer')
      .call(d3.drag()
          .on('start', (event, d) => {
            if (!event.active) simulation.alphaTarget(0.3).restart()
            d.fx = d.x; d.fy = d.y
          })
          .on('drag', (event, d) => { d.fx = event.x; d.fy = event.y })
          .on('end', (event, d) => {
            if (!event.active) simulation.alphaTarget(0)
            d.fx = null; d.fy = null
          }))
      .on('mouseover', function() {
        d3.select(this).attr('fill', COLOR_NODE_HOVER).attr('r', 16)
      })
      .on('mouseout', function() {
        d3.select(this).attr('fill', COLOR_NODE).attr('r', 12)
      })
      .on('click', (event, d) => {
        window.location.href = `/node/${d.id}`
      })

  const label = svg.append('g')
      .selectAll('text')
      .data(data.value.nodes)
      .join('text')
      .text(d => d.title)
      .attr('font-size', 11)
      .attr('fill', '#E0E7FF')
      .attr('text-anchor', 'middle')
      .attr('dy', -18)
      .style('pointer-events', 'none')

  simulation.on('tick', () => {
    link
        .attr('x1', d => d.source.x)
        .attr('y1', d => d.source.y)
        .attr('x2', d => d.target.x)
        .attr('y2', d => d.target.y)
    node
        .attr('cx', d => d.x)
        .attr('cy', d => d.y)
    label
        .attr('x', d => d.x)
        .attr('y', d => d.y)
  })
}
</script>

<style scoped>
.graph-view {
  width: 100%;
  height: 100vh;
  background: linear-gradient(135deg, #0A0E1A 0%, #1A1F3A 100%);
  position: relative;
}
.graph-svg {
  width: 100%;
  height: calc(100vh - 60px);
}
.page-title {
  color: #00E5FF;
  padding: 16px;
  margin: 0;
  font-weight: 300;
  letter-spacing: 2px;
}
.empty {
  position: absolute;
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  padding: 32px;
  text-align: center;
  color: #E0E7FF;
}
.btn-primary {
  display: inline-block;
  margin-top: 16px;
  padding: 8px 24px;
  background: rgba(0, 229, 255, 0.2);
  border: 1px solid #00E5FF;
  color: #00E5FF;
  border-radius: 8px;
  text-decoration: none;
}
.loading {
  color: #00E5FF;
  text-align: center;
  padding: 80px;
}
</style>
```

### 5.3 加路由

在 `src/router/index.js` 加:

```js
{
  path: '/graph',
  name: 'GraphView',
  component: () => import('@/views/GraphView.vue')
}
```

### 5.4 导航栏加链接

在主导航栏加 "图谱" 链接到 `/graph`。

## 6. 测试要求

1. 用户至少有 3 个节点 + 2 条边
2. 访问 `/graph`,应看到节点和边的图谱
3. 拖动节点,其他节点应跟随物理运动
4. 点击节点应跳转到节点详情页
5. 删除所有节点后再访问,应显示空状态
6. 节点 > 100 时仍能流畅(不卡顿)

## 7. 不做的事

- **不**做"按类型筛选节点"(UI 上加 checkbox,本期不做)
- **不**做"边的颜色按 edgeType 分"(本期所有边同色)
- **不**做"聚类/分群"算法
- **不**做"实时刷新"——只在打开页面时拉一次数据
- **不**导出 PNG
