# Cursor 任务总览

> **项目**: linkedknowledge 后端 + mindmap-ui 前端
> **目标**: 把现有 mindmap 重构成 Obsidian 风格的个人知识库
> **风格**: 液态科技风(玻璃质感、流光、霓虹描边)
> **数据**: 用户已确认无真实数据,可任意改 schema
> **JPA 配置**: `spring.jpa.hibernate.ddl-auto=update`(改字段会自动生效,但**请同时在文档里写出 ALTER TABLE 脚本**,以防 update 没生效)

---

## 任务总览

| ID | 标题 | 优先级 | 工作量(估) | 前置依赖 |
|---|---|---|---|---|
| **P0** | 数据模型重构:边表 + AI 建议表 | 必须先做 | 0.5 天 | 无 |
| **P1** | 反向链接 + 双向链接解析 | 必须先做 | 0.5 天 | P0 |
| **P2** | AI 联想边(关键词相似度版)+ 强化学习偏好存储 | 必须先做 | 1 天 | P0 |
| **P3** | 全局图谱视图(前端) | 中 | 1 天 | P0, P1 |
| **P4** | 标签系统重构(`@tag` 解析) | 中 | 0.5 天 | P0 |
| **P5** | 液态科技风 UI 全站改版 | 中 | 1 天 | 无 |
| **P6** | (可选) Daily Note + Markdown 块引用 | 后期 | 3 天+ | P0, P3 |

---

## 执行规则

1. **每个 Px 单独一个 Cursor session**,不要在一个 session 里跑多个 Px
2. **每个 Px 开始前**:先 `git checkout -b feature/pX-xxx`,做完 `git commit` 再合
3. **每个 Px 完成后**:跑完 `mvn test` + 前端 `npm run build` + 启动后端真实接口测一遍,再给用户演示
4. **不要触碰 user 表 / jwt / SecurityConfig**——这些是已有功能,Px 不重构
5. **前端不要换框架**(保持 Vue 3 + Konva),只改样式和组件

---

## 液态科技风 UI 规范(适用于所有前端 Px)

| 元素 | 规范 |
|---|---|
| 主色 | 冷蓝青(`#00E5FF`) + 深空黑(`#0A0E1A`) |
| 强调色 | 霓虹紫(`#A855F7`) / 警示橙(`#FF6B35`) |
| 玻璃质感 | `backdrop-filter: blur(20px)` + 半透明白底 |
| 流光 | 按钮 hover 时一道 1px 青光横扫,200ms |
| 描边 | `1px solid rgba(0,229,255,0.3)`,hover 时变亮 |
| 字体 | Inter / 系统 sans-serif,代码用 JetBrains Mono |
| 圆角 | 12px(卡片)/ 8px(按钮) |
| 阴影 | `0 8px 32px rgba(0,229,255,0.1)` |
| 动效 | 缓动统一 `cubic-bezier(0.4, 0, 0.2, 1)`,时长 200-400ms |

详细规范见 `P5-liquid-tech-style.md`。

---

## 6 份文档索引

- `P0-data-model-edges.md`
- `P1-bi-directional-links.md`
- `P2-ai-edge-suggestion-rl.md`
- `P3-graph-view-frontend.md`
- `P4-tag-system.md`
- `P5-liquid-tech-style.md`
- `P6-daily-note-blocks.md`(可选,本期可不做)

每份文档结构相同:
1. 目标 + 验收标准(可勾选)
2. 当前代码现状(供 Cursor 定位)
3. 后端改动(如有,具体到类名/方法名/字段)
4. 前端改动(如有)
5. 测试要求
6. 不做的事(明确边界)
