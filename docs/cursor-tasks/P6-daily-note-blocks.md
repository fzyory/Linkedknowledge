# P6 — Daily Note + 块引用(后期,本期可不做)

> **状态**:本期**不实现**。本文档作为下一期规划保留。
>
> 原因:这两个功能是 Obsidian 最复杂的部分(块级 ID、跨文档引用、自动 Daily Note 创建),工作量至少 3-5 天,且需要换编辑器(Monaco / CodeMirror),不在本期重构范围。

---

## 1. 目标(本期不实现,仅供理解)

### 1.1 Daily Note

- 用户打开应用,如果今天还没建 Daily Note,自动创建一个标题为 "2026-09-04" 的节点
- 模板可选(用户可配置)
- 应用启动后**自动跳转到**今天的 Daily Note

### 1.2 块引用 `[[笔记#块id]]`

- 节点内容里的每一段(Markdown 段落、列表项、标题)自动获得块 ID
- 用户能 `[[节点A#^abc123]]` 引用节点 A 里 abc123 那个块
- 反向链接面板:点击块引用,跳转到那个具体段落并高亮

## 2. 估时

- Daily Note: 0.5 天
- 块 ID 自动生成: 1 天
- 块引用解析 + 跳转: 1.5 天
- 编辑器升级到 Monaco: 1 天
- **合计: 4 天**

## 3. 后期再做时的设计要点(给下一期 Cursor 看的备忘)

- `node_block` 表:`id`, `node_id`, `block_uuid`(8 位短 ID), `content`, `order_index`
- `[[节点A#^abc123]]` 解析时,先按 `^` 取短 ID,再查 `node_block.block_uuid`
- Daily Note 模板存在 `system_config` 表里,key=`daily_note_template`
- 自动跳转逻辑在 `App.vue` 的 `onMounted` 里:查 `knowledge_node where title=today's date`,没有就 create,再 `router.push`

## 4. 本期 P0-P5 阶段**禁止**做的事

- 不实现块 ID
- 不实现 Daily Note
- 不升级编辑器(继续用 textarea)
- 不在 `KnowledgeNode` 实体加 block 相关字段
