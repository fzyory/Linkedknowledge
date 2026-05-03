# Spring Boot 配置知识

记录 Spring Boot 配置相关的知识点。

---

## application.yml 配置

### 数据源配置
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/linkedknowledge
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### JPA 配置
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update  # 自动更新表结构
    show-sql: true      # 显示 SQL 语句
```

**知识点**：
- `ddl-auto: update` 会根据实体类自动创建/更新表
- `show-sql: true` 用于调试，生产环境应关闭

---
