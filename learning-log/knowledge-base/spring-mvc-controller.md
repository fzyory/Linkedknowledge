# Spring MVC Controller 知识

记录 Spring MVC Controller 相关的知识点。

---

## 基本注解

### @RestController
```java
@RestController
public class HelloController {
    // 自动将返回值转为 JSON
}
```

**作用**：
- 相当于 `@Controller + @ResponseBody`
- 返回的数据自动转为 JSON 格式

### @GetMapping
```java
@GetMapping("/hello")
public String sayHello() {
    return "Hello, Linked Knowledge!";
}
```

**作用**：
- 处理 HTTP GET 请求
- 映射 URL 路径

---

## RESTful API 设计

### 标准的 CRUD 接口
```java
GET    /api/nodes          # 获取所有
GET    /api/nodes/{id}     # 获取单个
POST   /api/nodes          # 创建
PUT    /api/nodes/{id}     # 更新
DELETE /api/nodes/{id}     # 删除
```

---
