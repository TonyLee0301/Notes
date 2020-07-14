ApplicationListener 是Spring Framework context 模块下的一个接口 是应用程序事件监听器，可以通用地声明它感兴趣的事件类型，事件将被相应地过滤，只会调用侦听器来匹配事件对象
Spring Boot 默认的 ApplicationListener 由以下实现
* org.springframework.boot.ClearCachesApplicationListener
&emsp;&emsp;主要用于清空所有加载过后的context的缓存 主要监听的事件 ContextRefreshedEvent 是context刷新事件
* org.springframework.boot.builder.ParentContextCloserApplicationListener
&emsp;&emsp;
* org.springframework.boot.context.FileEncodingApplicationListener
* org.springframework.boot.context.config.AnsiOutputApplicationListener
* org.springframework.boot.context.config.ConfigFileApplicationListener
* org.springframework.boot.context.config.DelegatingApplicationListener
* org.springframework.boot.context.logging.ClasspathLoggingApplicationListener
* org.springframework.boot.context.logging.LoggingApplicationListener
* org.springframework.boot.liquibase.LiquibaseServiceLocatorApplicationListener