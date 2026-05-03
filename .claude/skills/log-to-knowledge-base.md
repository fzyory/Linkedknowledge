---
name: log-to-knowledge-base
description: 分析 Git 变更，将操作记录和错误总结写入双知识库。
---

# 知识库记录技能

当触发此技能时，请执行以下操作：

## 1. 读取骨架
读取 `knowledge-skeleton.txt`，确认当前学习路径。

## 2. 分析 Git 变更
运行 `git diff HEAD` 或 `git status`，分析变更：
- 修改了哪个 Java 类？
- 修改了 `pom.xml` 还是 `application.yml`？
- 是否有新文件或删除？

## 3. 写入【详细过程库】 (learning-log/detail-process.md)
追加以下内容（**不要覆盖**）：

```markdown
---
## [时间戳] 操作记录

### 当前目标
[从骨架中提取当前学习目标]

### 执行的操作
- 修改了 XXX 文件
- 添加了 XXX 代码
- 运行了 XXX 命令

### 遇到的问题
[如果有错误，详细记录]

### 解决方案
[如何解决的]

### Git 变更摘要
```
[git diff 输出]
```
---
```

## 4. 写入【问题总结库】 (learning-log/problem-summary.md)
**仅在检测到错误或 Bug 修复时触发**。追加：

```markdown
---
## [时间戳] 问题：[简短标题]

**问题现象**：
[错误信息或异常行为]

**根本原因**：
[为什么会出现这个问题]

**解决方案**：
[具体的修复步骤]

**相关知识点**：
- [涉及的技术概念]
- [需要理解的原理]

**预防措施**：
[下次如何避免]
---
```

## 5. 更新【结构化知识库】 (learning-log/knowledge-base/)
根据变更类型，更新对应的知识文件：

- 如果修改了实体类 → 更新 `jpa-entity-design.md`
- 如果修改了 Controller → 更新 `spring-mvc-controller.md`
- 如果修改了配置文件 → 更新 `spring-boot-config.md`
- 如果遇到错误 → 更新 `common-errors.md`

## 6. 输出总结
向用户报告：
- 记录了什么操作
- 更新了哪些知识文件
- 是否记录了问题
- 当前学习进度（参考骨架）
