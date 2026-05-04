# 问题总结库

专门记录遇到的错误、Bug 和解决方案。

---

## 2026-05-03 Java 版本不匹配

**问题现象**：
```
[ERROR] Fatal error compiling: 无效的标记: --release
```

**根本原因**：
- Maven 使用系统的 JAVA_HOME（Java 8）
- 项目需要 Java 17
- maven-compiler-plugin 的 --release 参数在 Java 8 中不支持

**解决方案**：
1. 在 IDEA 中配置 Maven Runner 使用 Java 17
2. 创建 `.mvn/jvm.config` 指定 Java 17 路径
3. 或使用 `use-java17.bat` 脚本临时切换环境

**相关知识点**：
- Maven 编译器插件的工作原理
- JAVA_HOME 环境变量的作用
- IDEA 的 JDK 配置机制

**预防措施**：
- 新项目创建时立即检查 JDK 版本
- 确保 IDEA 和命令行使用相同的 Java 版本

---

