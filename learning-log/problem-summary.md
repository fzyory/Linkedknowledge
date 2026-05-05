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


---

## 2026-05-05 Java 版本不匹配（续）— mvn spring-boot:run 启动失败

**问题现象：**
```
$ mvn spring-boot:run
# 项目无法启动（或编译报错）
```

**根本原因：**
- 系统 `mvn` 使用系统环境变量 JAVA_HOME（Java 8）
- 项目需要 Java 17（Spring Boot 3.x + pom.xml 配置）
- `use-java17.bat` 开的是新 cmd 窗口，在 PowerShell 中无效

**解决方案（不修改系统变量）：**
```powershell
$env:JAVA_HOME = "C:\Users\g\.jdks\ms-17.0.19"
cd ~\Desktop\linkedknowledge
.\mvnw.cmd spring-boot:run
```

**关键区别（vs 上次）：**
- 上次用 `mvn`（系统 Maven），这次用 `mvnw`（Maven Wrapper）
- Maven Wrapper 会读取 `.mvn/jvm.config` 和 `pom.xml`，是项目自带的正确入口
- `.mvn/jvm.config` 里配了 `-Djava.home=...` 但实际不生效，仍需设 `$env:JAVA_HOME`
- 临时 env var 只在当前 PowerShell 窗口有效，关闭即恢复

**相关知识点：**
- Maven Wrapper（`mvnw`）vs 系统 Maven（`mvn`）的区别
- `$env:JAVA_HOME` 作用域：仅当前 session
- IDEA Terminal vs 系统 PowerShell 的环境差异

**预防措施：**
- 以后从命令行跑项目，直接用 `mvnw.cmd` 代替 `mvn`
- 启动前置步骤：设 JAVA_HOME → 用 mvnw 跑

