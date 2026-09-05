// [框架]    本文件是 LinkedKnowledge 项目的主类入口。整个项目只有一个 main 方法,
//             启动后 Spring Boot 自动扫描 com.LinkedKnowledge 包下所有 @Component /
//             @Service / @Repository / @Controller / @Configuration / @RestControllerAdvice,
//             自动装配依赖、初始化数据库连接池、启动内嵌 Tomcat。
// [依赖]    org.springframework.boot.SpringApplication — Spring Boot 启动器,负责创建 ApplicationContext。
//             org.springframework.boot.autoconfigure.SpringBootApplication — 复合注解,等同于
//             @Configuration + @EnableAutoConfiguration + @ComponentScan。
// [入口]    通过 `mvn spring-boot:run` 或 `java -jar xxx.jar` 触发 main()。
//             启动后默认监听 8080 端口(application.yml 配置)。
//             启动顺序: 创建上下文 → 扫描组件 → 初始化数据库连接池 → 启动内嵌 Tomcat → 监听 HTTP 请求。
// [框架妙用] (1) @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan 三合一,
//              Spring Boot 自动检测 classpath 里的依赖并配置对应 Bean, 极大减少 XML 配置。
//             (2) SpringApplication.run() 是 Spring Boot 推荐的"启动 + 立即返回"方式,
//              返回的 ConfigurableApplicationContext 可以后续 close() 优雅关闭。
// [注意]    项目包名 com.LinkedKnowledge 是大写 L, 这是历史遗留(初次创建时 IDE 默认大写),
//             后续所有子包都以 LinkedKnowledge 开头(不是 linkedknowledge), 注释和搜索时注意。

package com.LinkedKnowledge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

// [入口]    项目启动入口。args 是命令行参数, Spring Boot 会自动解析 --server.port=xxx 等。
// [返回]    实际不返回值, 启动成功后进程常驻监听 HTTP 请求, 直到收到 SIGTERM 才退出。
@SpringBootApplication
@EnableAsync
public class LinkedKnowledgeApplication {

	// [实现]    标准 Spring Boot 启动写法。SpringApplication.run() 内部会:
	//             1) 创建 Spring 应用上下文 (AnnotationConfigApplicationContext)
	//             2) 加载 application.yml 配置
	//             3) 扫描并注册所有 Bean
	//             4) 启动内嵌 Tomcat / Jetty
	//             5) 注册 JVM shutdown hook, 收到 SIGTERM 时优雅关闭
	public static void main(String[] args) {
		SpringApplication.run(LinkedKnowledgeApplication.class, args);
	}

}
