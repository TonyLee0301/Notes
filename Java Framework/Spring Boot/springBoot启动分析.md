# 1. SpringBoot 启动流程源码分析
> 该spring boot 的源码是在 2.2.x 的基础上分析的。


<!-- @import "[TOC]" {cmd="toc" depthFrom=1 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->

- [1. SpringBoot 启动流程源码分析](#1-springboot-启动流程源码分析)
  - [1.1 启动类的初始化](#11-启动类的初始化)
    - [1.1.1 判断应用](#111-判断应用)
    - [1.1.2 Spring 内部的 SPI 机制](#112-spring-内部的-spi-机制)
  - [1.2 初始化 Initializer](#12-初始化-initializer)
  - [1.2 监听 Listener](#12-监听-listener)
  - [1.3 springboot 启动](#13-springboot-启动)
    - [1.3.1 java.awt.headless](#131-javaawtheadless)
    - [1.3.2 创建 SpringApplicationRunListeners](#132-创建-springapplicationrunlisteners)
    - [1.3.3 创建默认的参数](#133-创建默认的参数)
    - [1.3.4 准备环境](#134-准备环境)
      - [1. 创建或获取环境：](#1-创建或获取环境)
      - [2. 配置环境](#2-配置环境)
      - [3. 将 ConfigurationPropertySources 支持附加到指定的环境上](#3-将-configurationpropertysources-支持附加到指定的环境上)
      - [4. 发布环境准备由相关监听器处理相关逻辑，主要用于首次环境准备或检查](#4-发布环境准备由相关监听器处理相关逻辑主要用于首次环境准备或检查)
      - [5. 绑定环境到 springApplication](#5-绑定环境到-springapplication)
      - [6. 将 ConfigurationPropertySources 配置附加到指定的环境上](#6-将-configurationpropertysources-配置附加到指定的环境上)

<!-- /code_chunk_output -->


## 1.1 启动类的初始化
 ```java
 @SpringBootApplication
 public class CountingTimesApplication {
    public static void main(String... args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
 ```
 spring提供了一个run的静态方法，传递Main.class，启动参数，我们在平时使用过程中，就可以直接写对应的main函数，并调用即可。
 接下来，我们看下静态的run方法主要做了什么
 ```java
 public static ConfigurableApplicationContext run(Class<?>[] primarySources, String[] args) {
	 	//初始化 SpringApplication 并调用 run 方法
		return new SpringApplication(primarySources).run(args);
	}
 ``` 
 >初始化 SpringApplication 对象
 ```java
 public SpringApplication(Class<?>... primarySources) {
		this(null, primarySources);
}
 ```
 ```java
 public SpringApplication(ResourceLoader resourceLoader, Class<?>... primarySources) {
		this.resourceLoader = resourceLoader;
		Assert.notNull(primarySources, "PrimarySources must not be null");
		this.primarySources = new LinkedHashSet<>(Arrays.asList(primarySources));
		//判断web应用类型 NON 非web应用 SERVLET 普通给予servlet的web应用 ，REACTIVE 给予reactive的web应用
		//判断方法是去通过classLoader加载对应的基础类
		this.webApplicationType = WebApplicationType.deduceFromClasspath();
		//设置 初始化器 通过加载 META-INF/spring.factories 下的配置文件 查找 ApplicationContextInitializer
		setInitializers((Collection) getSpringFactoriesInstances(ApplicationContextInitializer.class));
		//设置 监听器 同样是通过加载 META-INF/spring.factories 下的配置文件 查找 ApplicationListener 类
		setListeners((Collection) getSpringFactoriesInstances(ApplicationListener.class));
		//判断 main 函数所在的class
		this.mainApplicationClass = deduceMainApplicationClass();
	}
 ```
通过上面的代码，我们不难发现，初始化 SpringApplication 主要做了以下几件事：
1. 设置设置 resourceLoader, 这里的 resourceLoader 可以理解为所有的资源，不管是配置、class或者是通过网络下载的。
2. 设置启动主类。
3. 判断当前应用是什么应用，web reactive 或者普通的应用
4. 设置启动类
5. 设置监听器

### 1.1.1 判断应用
Spring是通过什么方式来判断我们的应用程序是什么应用呢？
其实说来也简单，就是查找对应class资源中，是否有该应用的class

WebApplicationType.deduceFromClasspath()

 ```java
 static WebApplicationType deduceFromClasspath() {
		// org.springframework.web.reactive.DispatcherHandler 存在，并且 org.springframework.web.servlet.DispatcherServlet 不存在，则是 reactive 应用
		if (ClassUtils.isPresent(WEBFLUX_INDICATOR_CLASS, null) && !ClassUtils.isPresent(WEBMVC_INDICATOR_CLASS, null)
				&& !ClassUtils.isPresent(JERSEY_INDICATOR_CLASS, null)) {
			return WebApplicationType.REACTIVE;
		}
		//判断 如果 数组中的{ "javax.servlet.Servlet",
		//			"org.springframework.web.context.ConfigurableWebApplicationContext" }
		//都不存在则为 非web 应用
		for (String className : SERVLET_INDICATOR_CLASSES) {
			if (!ClassUtils.isPresent(className, null)) {
				return WebApplicationType.NONE;
			}
		}
		//默认 servlet 应用
		return WebApplicationType.SERVLET;
	}
 ```
### 1.1.2 Spring 内部的 SPI 机制
应用判断完成了，接下来，就应该设置初始化器和监听器了，细心的同学其实已经看到了设置对应的初始化器或监听器 都是通过一个方法 getSpringFactoriesInstances 来实现的，我们就具体来看看该方法的实现：
 ```java
    //获取 spring factory 实例
    private <T> Collection<T> getSpringFactoriesInstances(Class<T> type) {
		return getSpringFactoriesInstances(type, new Class<?>[] {});
	}

    private <T> Collection<T> getSpringFactoriesInstances(Class<T> type, Class<?>[] parameterTypes, Object... args) {
        //获取当前的classLoader
		ClassLoader classLoader = getClassLoader();
		// Use names and ensure unique to protect against duplicates
        // 加载对应的配置文件 和 获取对应 实现类
		Set<String> names = new LinkedHashSet<>(SpringFactoriesLoader.loadFactoryNames(type, classLoader));
        //创建对应的实例，排序
		List<T> instances = createSpringFactoriesInstances(type, parameterTypes, classLoader, args, names);
		AnnotationAwareOrderComparator.sort(instances);
		return instances;
	}
 ```
可以看出以上代码主要就做了几点工作：
1、获取 classLoader。并通过 classLoader 加载对应的资源文件。获取当前 指定的接口、抽象类的 实现 class 全称。
2、通过 createSpringFactoriesInstances 方法，将获取到的 class 实例化。
3、排序，并返回。

下面我们再来看下 SpringFactoriesLoader.loadFactoryNames(type, classLoader)方法
> SpringFactoriesLoader
 ```java
    /**
	 * 使用给定的class loader 从 META-INF/spring.factories 加载实现给定类型的 class全称
	 * @param factoryType
	 * @param classLoader
	 * @return
	 */
	public static List<String> loadFactoryNames(Class<?> factoryType, @Nullable ClassLoader classLoader) {
		String factoryTypeName = factoryType.getName();
		return loadSpringFactories(classLoader).getOrDefault(factoryTypeName, Collections.emptyList());
	}
 ```
其中 loadSpringFactories(classLoader) 就不做详细讲解了，其主要功能：根据 classLoader 获取从 ConcurrentReferenceHashMap 对象中获取对应的资源文件，以 factoryType 为 key 对应实现 factoryType 的class全称的集合为 value ，没有则通过 classLoader 加载。

createSpringFactoriesInstances(type, parameterTypes, classLoader, args, names) 即通过反射实例化。具体的调用在 BeanUtils.instantiateClass 中，有兴趣的同学可以看看。

看了上面的介绍，是否和SPI机制有点相像？
>SPI机制：为某个接口寻找服务实现的机制。有点类似IOC的思想，就是将装配的控制权移到程序之外，在模块化设计中这个机制尤其重要。

spring 通过 META-INF/spring.factories 资源的加载，对应接口的实现，并将其实例化，也就是我们常说的SPI机制，这就是实现spring内部自己的SPI机制。

因为 spring 框架分为了很多的相关模块组件，因此每个对应的组件，可能都有一些相关的初始化器，需要进行处理。因此这里使用spring 的 SPI 机制，可以解决模块插拔的问题。

## 1.2 初始化 Initializer
我们先来看看初始化器接口 `ApplicationContextInitializer` 的定义
 ```java
 /**
 * 在 ConfigurableApplicationContext refresh() 调用之前,初始化 Spring ConfigurableApplicationContext 的回调接口
 * @param <C>
 */
public interface ApplicationContextInitializer<C extends ConfigurableApplicationContext> {

	/**
	 * Initialize the given application context.
	 * @param applicationContext the application to configure
	 */
	//初始化 application context
	void initialize(C applicationContext);

}
 ```
其实就一个方法，initialize(C applicationContext) 参数为 ConfigurableApplicationContext 的子类。就是做相关模块或者组件的初始化工作。
下面我们来简单看下，spring boot 默认有哪些 Initializer 的实现提供：
 ```properties
 # Application Context Initializers
org.springframework.context.ApplicationContextInitializer=\
org.springframework.boot.context.ConfigurationWarningsApplicationContextInitializer,\
org.springframework.boot.context.ContextIdApplicationContextInitializer,\
org.springframework.boot.context.config.DelegatingApplicationContextInitializer,\
org.springframework.boot.rsocket.context.RSocketPortInfoApplicationContextInitializer,\
org.springframework.boot.web.context.ServerPortInfoApplicationContextInitializer
 ```
|实现类|简介|
|---|---|
|ConfigurationWarningsApplicationContextInitializer|向application context 中添加，警告ConfigurationWarningsPostProcessor|
|ContextIdApplicationContextInitializer|设置 contextId 并注册|
|DelegatingApplicationContextInitializer|代理在 `environment` 环境下特定的 `context.initializer.classes` 属性配置的 initializer|
|RSocketPortInfoApplicationContextInitializer|设置启动端口信息|
|ServerPortInfoApplicationContextInitializer|设置启动端口信息|

## 1.2 监听 Listener
下面我们再来看看 ApplicationListener 监听器又做可以做什么，在什么时候使用。
 ```java
 @FunctionalInterface
 public interface ApplicationListener<E extends ApplicationEvent> extends EventListener {

	/**
	 * Handle an application event.
	 * @param event the event to respond to
	 */
	void onApplicationEvent(E event);

 }
 ```
可以看到 ApplicationListener 只定义了一个方法， onApplicationEvent(E event) 用于响应事件。 该接口是用于被实现对应事件的监听，那应该spring 默认的事件是哪些呢？

我们再来看看 GenericApplicationListener
 ```java
 /**
 * 基础 ApplicationListener 接口的扩展类型，公开如 支持 事件和资源 类型 的元数据/方法
 */
 public interface GenericApplicationListener extends ApplicationListener<ApplicationEvent>, Ordered {

	/**
	 * Determine whether this listener actually supports the given event type.
	 * 决定该 listener 是否支持 该给定的 事件类型
	 * @param eventType the event type (never {@code null})
	 */
	
	boolean supportsEventType(ResolvableType eventType);

	/**
	 * Determine whether this listener actually supports the given source type.
	 * 决定该 listener 是否支持 该给 Class 的资源类型
	 * <p>The default implementation always returns {@code true}.
	 * @param sourceType the source type, or {@code null} if no source
	 */
	default boolean supportsSourceType(@Nullable Class<?> sourceType) {
		return true;
	}

	/**
	 * Determine this listener's order in a set of listeners for the same event.
	 * <p>The default implementation returns {@link #LOWEST_PRECEDENCE}.
	 */
	@Override
	default int getOrder() {
		return LOWEST_PRECEDENCE;
	}

}
 ```
可以看到该接口扩展了`ApplicationListener`并定义了两个方法，判断该 `listener` 支持的 事件类型 和 资源类型。实现该接口的监听器，就可以自己决定监听哪些具体的事件了。

现在我们再来看看相关的事件：
我们先找应用上下文相关的事件。先看下面的代码：

 ```java
  public abstract class ApplicationContextEvent extends ApplicationEvent{
	 /**
	 * Create a new ContextStartedEvent.
	 * @param source the {@code ApplicationContext} that the event is raised for
	 * (must not be {@code null})
	 */
	public ApplicationContextEvent(ApplicationContext source) {
		super(source);
	}

	/**
	 * Get the {@code ApplicationContext} that the event was raised for.
	 */
	public final ApplicationContext getApplicationContext() {
		return (ApplicationContext) getSource();
	}

 }
 ```
定义了一个 ApplicationContextEvent 应用上下文的事件，该 class 是一个抽象类，ApplicationContext的相关的事件的具体实现：
|实现类|事件|说明|
|--|--|--|
|ContextRefreshedEvent|应用程序上下文刷新事件|当 ApplicationContext 初始化或刷新时引发的事件
|ContextStartedEvent|应用程序上下文启动事件|当 ApplicationContext 启动后引发的事件
|ContextStoppedEvent|应用程序上下文停止事件|当 ApplicationContext 停止引发的事件
|ContextClosedEvent|应用程序上线关闭事件|当 ApplicationContext 关闭引发的事件

当然Spring的事件远远不止这些，后面我们在看相关监听器实现的时候可以看到还有很多其他的事件，在spring boot 启动过程中触发。

现在我们来看看 ApplicationListener 的默认实现。

查找META-INFO下的 spring.factories 文件，找到 org.springframework.context.ApplicationListener 的配置
在spring-boot中我们先看下默认的实现

 ```properties
 org.springframework.context.ApplicationListener=\
 org.springframework.boot.ClearCachesApplicationListener,\
 org.springframework.boot.builder.ParentContextCloserApplicationListener,\
 org.springframework.boot.context.FileEncodingApplicationListener,\
 org.springframework.boot.context.config.AnsiOutputApplicationListener,\
 org.springframework.boot.context.config.ConfigFileApplicationListener,\
 org.springframework.boot.context.config.DelegatingApplicationListener,\
 org.springframework.boot.context.logging.ClasspathLoggingApplicationListener,\
 org.springframework.boot.context.logging.LoggingApplicationListener,\
 org.springframework.boot.liquibase.LiquibaseServiceLocatorApplicationListener
 ```

在spring-boot-autoconfiguration中我们先看下常用的个配置

 ```properties
 org.springframework.context.ApplicationListener=\
 org.springframework.boot.autoconfigure.BackgroundPreinitializer
 ```
 
|监听器|监听事件|介绍|
|--|--|--|
|ClearCachesApplicationListener|ContextRefreshedEvent|上下文被加载，清除一次缓存|
|ParentContextCloserApplicationListener|ParentContextAvailableEvent| 当父上下文可用是触发 ，该监听器在父上下文可用是会创建一个 关闭当前上下文的监听器，并将该监听器注册到父上下文中，当父上下文关闭时，该监听器会传递关闭当前上下文。这个 listener 就是为了构建一个层级监听|
|FileEncodingApplicationListener|ApplicationEnvironmentPreparedEvent|监听应用程序环境准备完成事件，检查配置的文件编码时候和系统编码相同
|AnsiOutputApplicationListener|ApplicationEnvironmentPreparedEvent|同上监听，???看不懂啊。。
|ConfigFileApplicationListener|ApplicationEnvironmentPreparedEvent、ApplicationPreparedEvent|配置文件的listener，加载 spring.properties/spring.yml文件
|DelegatingApplicationListener|ApplicationEvent|代理在 `environment` 环境下特定的 `context.listener.classes` 属性配置的 listener
|ClasspathLoggingApplicationListener|ApplicationEnvironmentPreparedEvent、ApplicationFailedEvent|打印准备好环境的debug日志和应用启动失败的日志
|LoggingApplicationListener|ApplicationStartingEvent、ApplicationEnvironmentPreparedEvent、ApplicationPreparedEvent、ContextClosedEvent、ApplicationFailedEvent|配置的日志
|LiquibaseServiceLocatorApplicationListener|ApplicationStartingEvent|将liquibase ServiceLocator替换为与Spring引导可执行归档一起工作的版本

可以看出，spring boot 启动过程中各个阶段，都会有相关的事件需要对应的listener处理。每个listener都指负责单一的工作。虽然可能监听的事件有多个，但是处理的事情也是同一个工作。这也符合面向对象变成中`单一职责`的原则。

## 1.3 springboot 启动
启动前的一些初始化工作已经完成，接下来我们来看看启动
 ```java
 	/**
	 * Run the Spring application, creating and refreshing a new
	 * {@link ApplicationContext}.
	 * @param args the application arguments (usually passed from a Java main method)
	 * @return a running {@link ApplicationContext}
	 */
	public ConfigurableApplicationContext run(String... args) {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		ConfigurableApplicationContext context = null;
		Collection<SpringBootExceptionReporter> exceptionReporters = new ArrayList<>();
		//设置 java.awt.headless 为true
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
接下来，我们一步一步分析run方法中到底做了些什么

### 1.3.1 java.awt.headless
Headless模式是系统的一种配置模式。在系统可能缺少显示设备、键盘或鼠标这些外设的情况下可以使用该模式。
Headless模式虽然不是我们愿意见到的，但事实上我们却常常需要在该模式下工作，尤其是服务器端程序开发者。因为服务器（如提供Web服务的主机）往往可能缺少前述设备，但又需要使用他们提供的功能，生成相应的数据，以提供给客户端（如浏览器所在的配有相关的显示设备、键盘和鼠标的主机）。

### 1.3.2 创建 SpringApplicationRunListeners
我们具体来看下 SpringApplicationRunListeners 的创建
 ```java
 private SpringApplicationRunListeners getRunListeners(String[] args) {
		Class<?>[] types = new Class<?>[] { SpringApplication.class, String[].class };
		return new SpringApplicationRunListeners(logger,
				getSpringFactoriesInstances(SpringApplicationRunListener.class, types, this, args));
	}
 ```
又是 `getSpringFactoriesInstances` 方法，我们知道该方法，就是从 META-INF/spring.factories 中获取对应接口或抽象类的具体实现方法，并实例化；
需要注意的是，这里比之前创建 SpringListener 和 SpringInitialized 多了几个参数。其实就是参数就是构造方法参数的类型和参数，args是我们在启动调用run时传递进来的参数。
同时这里的 `this` 也把 `SpringApplication` 作为参数传递给了 正在实例化的 SpringApplicationRunListener。SpringApplicationRunListener 的具体作用就是在 spring 启动过程中发布各种事件,并通知各个 listener 处理相关事件。 
SpringApplicationRunListener的实现与相关事件的通知[具体可见](listener/SpringApplicationRunListener.md)

### 1.3.3 创建默认的参数
 ```java
ApplicationArguments applicationArguments = new DefaultApplicationArguments(args);
 ```
 说是默认，但其实，封装了相关的启动参数，也就是我们在启动命令的时候使用的相关命令行 例如: --server.port=9090

### 1.3.4 准备环境
 ```java
 	//准备环境
	ConfigurableEnvironment environment = prepareEnvironment(listeners, applicationArguments);

	private ConfigurableEnvironment prepareEnvironment(SpringApplicationRunListeners listeners,
			ApplicationArguments applicationArguments) {
		// Create and configure the environment
		//创建或者获取环境
		ConfigurableEnvironment environment = getOrCreateEnvironment();
		//配置环境
		configureEnvironment(environment, applicationArguments.getSourceArgs());
		//将 ConfigurationPropertySources 配置附加到指定的环境上
		ConfigurationPropertySources.attach(environment);
		//发布环境准备，主要用于首次环境准备或检查
		listeners.environmentPrepared(environment);
		//绑定环境到 springApplication
		bindToSpringApplication(environment);
		if (!this.isCustomEnvironment) {
			environment = new EnvironmentConverter(getClassLoader()).convertEnvironmentIfNecessary(environment,
					deduceEnvironmentClass());
		}
		//将 ConfigurationPropertySources 配置附加到指定的环境上
		ConfigurationPropertySources.attach(environment);
		return environment;
	}
 ```
讲到这里的时候，我们可能需要先准备了解一下，spring 和 spring boot 相关配置资源类。


可以看到准备环境主要有一下几步：
#### 1. 创建或获取环境：
 ```java
 	/**
	 * 根据不同的应用创建不同的环境
	 * @return
	 */
	private ConfigurableEnvironment getOrCreateEnvironment() {
		if (this.environment != null) {
			return this.environment;
		}
		switch (this.webApplicationType) {
		case SERVLET:
			return new StandardServletEnvironment();
		case REACTIVE:
			return new StandardReactiveWebEnvironment();
		default:
			return new StandardEnvironment();
		}
	}
 ```
#### 2. 配置环境
 ```java
	/**
	 * Template method delegating to
	 * {@link #configurePropertySources(ConfigurableEnvironment, String[])} and
	 * {@link #configureProfiles(ConfigurableEnvironment, String[])} in that order.
	 * Override this method for complete control over Environment customization, or one of
	 * the above for fine-grained control over property sources or profiles, respectively.
	 * @param environment this application's environment
	 * @param args arguments passed to the {@code run} method
	 * @see #configureProfiles(ConfigurableEnvironment, String[])
	 * @see #configurePropertySources(ConfigurableEnvironment, String[])
	 *
	 * 模板方法，通过顺序 委托给 configurePropertySources configureProfiles
	 */
	protected void configureEnvironment(ConfigurableEnvironment environment, String[] args) {
		//由于当前环境变量才刚创建，因此主要处理的相关环境，均是通过启动参数 args 传递过来的。
		if (this.addConversionService) {
			//获取 conversionService 并将 conversionService 设置到 environment 中
			//conversionService 设置相关的转换，格式化类，到环境中
			ConversionService conversionService = ApplicationConversionService.getSharedInstance();
			environment.setConversionService((ConfigurableConversionService) conversionService);
		}
		//添加/删除/修改propertySource,主要是将默认的 defaultProperties 和 启动参数 args 设置到 environment 当中
		configurePropertySources(environment, args);
		//配置 profile
		configureProfiles(environment, args);
	}
 ```
 环境的配置，其实主要做了2个事件，1、首先是配置和创建类型环境变量的一些类型转换类；2、根据启动参数，设置对应的环境配置和profile配置

#### 3. 将 ConfigurationPropertySources 支持附加到指定的环境上
其实这里做的事，将原本 environment 中的 MutablePropertySources 转换成 ConfigurationPropertySourcesPropertySource。
具体什么作用，暂时还未详细解读。
 ```java
 public static void attach(Environment environment) {
		Assert.isInstanceOf(ConfigurableEnvironment.class, environment);
		MutablePropertySources sources = ((ConfigurableEnvironment) environment).getPropertySources();
		PropertySource<?> attached = sources.get(ATTACHED_PROPERTY_SOURCE_NAME);
		if (attached != null && attached.getSource() != sources) {
			sources.remove(ATTACHED_PROPERTY_SOURCE_NAME);
			attached = null;
		}
		if (attached == null) {
			sources.addFirst(new ConfigurationPropertySourcesPropertySource(ATTACHED_PROPERTY_SOURCE_NAME,
					new SpringConfigurationPropertySources(sources)));
		}
	}
 ```
#### 4. 发布环境准备由相关监听器处理相关逻辑，主要用于首次环境准备或检查
通过 SpringRunListener 和 EventPublishingRunListener 广播发送 ApplicationEnvironmentPreparedEvent 事件。
主要几个事件监听器为：
* FileEncodingApplicationListener
FileEncodingApplicationListener 作用主要是检查文件编码  
&emsp;&emsp;判断 spring.mandatory-file-encoding 如果存在 和 jvm 环境的 file.encoding 是否相同，不同则抛出异常
AnsiOutputApplicationListener
&emsp;&emsp;设置 AnsiOutput 的相关配置
* ConfigFileApplicationListener
ClasspathLoggingApplicationListener
LoggingApplicationListener

#### 5. 绑定环境到 springApplication
#### 6. 将 ConfigurationPropertySources 配置附加到指定的环境上




