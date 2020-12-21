# 14 Spring Boot 体系原理

<!-- @import "[TOC]" {cmd="toc" depthFrom=1 depthTo=6 orderedList=false} -->

&emsp;&emsp;Spring Boot 是由 Pivotal 团队提供的全新框架，其设计目的是用来简化新 Spring 应用的初始搭建以及开发过程。该框架使用了特定的方式来进行配置，从而使开发人员不再需要定义样板化的配置。通过这种方式，Spring Boot 将致力于在蓬勃发展的快速应用开发领域(Rapid Application Developmenet)成为领导者。
&emsp;&emsp;Spring Boot 特点如下：
* 创建独立的Spring应用程序；
* 嵌入的Tomcat，无需部署WAR文件；
* 简化 Maven 配置；
* 自动配置 Spring；
* 提供生产就绪行功能，如指标、健康检查和外部配置；
* 绝对没有代码生成，以及对XML没有配置要求。
&emsp;&emsp;当然，这样的介绍似乎太官方化，好像并没有帮助我们理解Spring Boot 到底做了什么，我们不妨通过一个小例子来快速了解 Spring Boot。
&emsp;&emsp;首先我们搭建一个maven工程，pom 如下：
 ```xml
 <?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>spring-boot-starter-parent</artifactId>
        <groupId>org.springframework.boot</groupId>
        <version>2.0.1.RELEASE</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>spring-boot</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
 ```
&emsp;&emsp;建立一个controller类：
 ```java
package info.tonylee.studio.spring.boot;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @RequestMapping("/")
    String home(){
        return "Hello Spring Boot";
    }
}
 ```
&emsp;&emsp;最后我们再加入启动整个项目的main函数：
 ```java
package info.tonylee.studio.spring.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringBootDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringBootDemoApplication.class);
    }
}
 ```
&emsp;&emsp;以上就是我们要准备的示例内容，尝试启动main函数，并在浏览器中输入localhost:8080。
&emsp;&emsp;一切都非常方便与简洁，与之前的经验，如果要构建这样一套MVC体系是非常繁琐的。需要引入大堆pom依赖，同时，配置web.xml，编写spring配置xml。启动需要依赖Tomca同等容器。
&emsp;&emsp;在我们正式进入 Spring Boot 原理探索之前，我们首先还是尝试去下载已经安装其源码。
## 14.1 Spring Boot 源码安装
&emsp;&emsp;同样 Spring Boot 通过 Github 维护。相关代码可以直接在Github上下载。
## 14.2 第一个 Starter
&emsp;&emsp;Spring Boot 之所以流行，是因为 spring starter 模式的提出。spring starter 的出现，可以让模块开发更加独立化，相互间依赖更加松散以及可以更加方便地集成。
&emsp;&emsp;我们定义一个接口，可以认为它是当前独立业务开发模块对外暴露的可以直接调用的接口，一个实现，如下：
 ```java
package info.tonylee.studio.spring.boot.starter;

public interface HelloService {
    public String sayHello();
}
 ```
 ```java
package info.tonylee.studio.spring.boot.starter;

import org.springframework.stereotype.Component;

@Component
public class HelloServiceImpl implements HelloService {
    @Override
    public String sayHello() {
        return "say hello spring boot!";
    }
}
 ```
&emsp;&emsp;然后再添加一个自动配置项：
 ```java
package info.tonylee.studio.spring.boot.starter;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("info.tonylee.studio.spring.boot.starter")
public class HelloServiceAutoConfiguration {
}
 ```
&emsp;&emsp;可以看到 HelloServiceAutoConfiguration 并没有逻辑实现，它存在的目的仅仅是通过注解进行配置的声明，我们可以在ComponentScan 中加入这个模块的容器扫描路径。
&emsp;&emsp;当然，如果仅仅是到此，Starter 还是没有完成开发，还需要最后一步，那就是声明这个配置文件的路径，在Spring的根路径下建立META-INF/spring.factories文件，并声明配置项路径。
 ```properties
 org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
  info.tonylee.studio.spring.boot.starter.HelloServiceAutoConfiguration
 ```
&emsp;&emsp;到此，一个标准的Starter就完成开发了，它有什么用或者说它怎么使用呢？我们来看一下它的使用方式。
&emsp;&emsp;在我们之前的Web项目中，加入它的maven依赖，同时更改Controller的逻辑，将模块的逻辑引入：
 ```java
@RestController
public class TestController {
    @Autowired
    private HelloService helloService;
    @RequestMapping("/")
    String home(){
//        return "Hello Spring Boot";
        return helloService.sayHello();
    }
}
 ```
&emsp;&emsp;可以发现我们只需要引入了模块的依赖，那么就可以直接通过接口注入，这样给模块开发带来了非常大的方便，同时为后续的模块拆分提供了遍历。
## 14.3 探索 SpringApplication 启动 Spring
&emsp;&emsp;我们找到主函数入口 SpringBootDemoApplication，发现这个入口的启动爱是比较奇怪的，这也是Spring Boot启动的必要做法，那么，这也可以作为我们分析Spring Boot的入口：
 ```java
@SpringBootApplication
public class SpringBootDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringBootDemoApplication.class);
    }
}
 ```
&emsp;&emsp;当顺着SpringApplication.run方法进入的时候我们找到了SpringApplication的一个看似核心逻辑的方法：
 ```java
    public ConfigurableApplicationContext run(String... args) {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		ConfigurableApplicationContext context = null;
		Collection<SpringBootExceptionReporter> exceptionReporters = new ArrayList<>();
		//设置 java.awt.headless 为true Headless模式是系统的一种配置模式。在系统可能缺少显示设备、键盘或鼠标这些外设的情况下可以使用该模式。
		configureHeadlessProperty();
		//创建SpringApplicationRunListener
		SpringApplicationRunListeners listeners = getRunListeners(args);
		//发送 ApplicationStartingEvent 事件
		listeners.starting();
		try {
			//创建一个默认的应用参数
			ApplicationArguments applicationArguments = new DefaultApplicationArguments(args);
			//准备环境
			ConfigurableEnvironment environment = prepareEnvironment(listeners, applicationArguments);
			//设置忽略的 spring.beaninfo.ignore beaninfo
			configureIgnoreBeanInfo(environment);
			//控制台打印banner
			Banner printedBanner = printBanner(environment);
			//创建 ApplicationContext 应用程序上下文
			context = createApplicationContext();
			//获取异常报告
			exceptionReporters = getSpringFactoriesInstances(SpringBootExceptionReporter.class,
					new Class[] { ConfigurableApplicationContext.class }, context);
			//准备 ApplicationContext 应用程序上下文
			prepareContext(context, environment, listeners, applicationArguments, printedBanner);
			//刷新 ApplicationContext 应用程序上下文
			refreshContext(context);
			//刷新后 ApplicationContext 应用程序上下文
			afterRefresh(context, applicationArguments);
			stopWatch.stop();
			if (this.logStartupInfo) {
				new StartupInfoLogger(this.mainApplicationClass).logStarted(getApplicationLog(), stopWatch);
			}
			//发送 ApplicationStartedEvent 事件
			listeners.started(context);
			//回调的runner
			callRunners(context, applicationArguments);
		}
		catch (Throwable ex) {
			handleRunFailure(context, ex, exceptionReporters, listeners);
			throw new IllegalStateException(ex);
		}

		try {
			//发送 ApplicationReadyEvent 事件
			listeners.running(context);
		}
		catch (Throwable ex) {
			handleRunFailure(context, ex, exceptionReporters, null);
			throw new IllegalStateException(ex);
		}
		return context;
	}
 ```
&emsp;&emsp;在这里，我们发现了几个关键字眼：
 ```java
    context = createApplicationContext();
    //刷新 ApplicationContext 应用程序上下文
    refreshContext(context);
    //刷新后 ApplicationContext 应用程序上下文
    afterRefresh(context, applicationArguments);
 ```
&emsp;&emsp;我们知道Spring完成的初始化方案，其中最为核心的就是 SpringContext 的创建、初始化、刷新等。那么我们可以直接查看其中的逻辑，同时Spring作为一个全球都在使用的框架，会有非常多需要考虑的问题，我们在阅读源码的过程中，只需要关心核心的主流程，了解其工作原理，并在阅读的过程中感受它的代码风格以及设计理念就好了。
### 14.3.1 SpringContext 创建
 ```java
    protected ConfigurableApplicationContext createApplicationContext() {
		Class<?> contextClass = this.applicationContextClass;
		if (contextClass == null) {
			try {
				switch (this.webApplicationType) {
				case SERVLET:
					contextClass = Class.forName(DEFAULT_SERVLET_WEB_CONTEXT_CLASS);
					break;
				case REACTIVE:
					contextClass = Class.forName(DEFAULT_REACTIVE_WEB_CONTEXT_CLASS);
					break;
				default:
					contextClass = Class.forName(DEFAULT_CONTEXT_CLASS);
				}
			}
			catch (ClassNotFoundException ex) {
				throw new IllegalStateException(
						"Unable create a default ApplicationContext, please specify an ApplicationContextClass", ex);
			}
		}
		return (ConfigurableApplicationContext) BeanUtils.instantiateClass(contextClass);
	}
 ```
&emsp;&emsp;