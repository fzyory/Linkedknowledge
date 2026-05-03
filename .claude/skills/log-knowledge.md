---
name: log-knowledge
description: 自动记录操作到双库，匹配骨架节点
---
1. 运行 `git diff HEAD~1 HEAD`（如果失败，则运行 `git diff HEAD`），获取本次提交变更。
2. 读取 `knowledge-skeleton.txt`，列出所有节点。
3. 提示用户："请输入本次操作关联的骨架节点（选1个）："
4. 读取用户输入的节点。
5. 往 `learning-log/detail-process.md` 追加：

```
时间戳：[自动生成]
操作：[从git commit message取]
变更文件：[从git diff取]
骨架节点：[用户输入]
思考路径：[用户后续补充]
```

6. 如果 commit message 含"fix/error/bug"，则往 `learning-log/problem-summary.md` 追加标准条目：

```
维度    详情
现象​    [从日志提取]
骨架位置​    [用户输入的节点]
根因​    [分析得出]
解决方案​    [具体改动]
关联知识点​    [相关技术点]
```

7. 输出："✅ 记录完成，请补充思考路径到 detail-process.md 最后一条。"
