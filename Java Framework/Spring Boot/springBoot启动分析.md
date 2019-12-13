# 1. SpringBoot 启动流程源码分析
> 该spring boot 的源码是在 2.2.x 的基础上分析的。

<!-- @import "[TOC]" {cmd="toc" depthFrom=1 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->

- [1. SpringBoot 启动流程源码分析](#1-springboot-启动流程源码分析)
  - [1.1 首先是启动类](#11-首先是启动类)
    - [1.1.1 初始化器](#111-初始化器)
      - [springboot ApplicationInitializer的默认实现](#springboot-applicationinitializer的默认实现)
    - [1.1.2 监听器](#112-监听器)
  - [1.2. springboot 启动](#12-springboot-启动)
    - [1.2.1 java.awt.headless](#121-javaawtheadless)
    - [1.2.2 创建 SpringApplicationRunListeners](#122-创建-springapplicationrunlisteners)
      - [1 SpringApplicationRunListener](#1-springapplicationrunlistener)
      - [2 EventPublishingRunListener](#2-eventpublishingrunlistener)
      - [3 SimpleApplicationEventMulticaster](#3-simpleapplicationeventmulticaster)
      - [3.1 方法 resolveDefaultEventType(event)](#31-方法-resolvedefaulteventtypeevent)
      - [3.1 方法 getApplicationListeners(event, type)](#31-方法-getapplicationlistenersevent-type)
    - [1.2.3 starting 事件](#123-starting-事件)

<!-- /code_chunk_output -->


## 1.1 首先是启动类
 ```java
 @SpringBootApplication
 public class CountingTimesApplication {
    public static void main(String... args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
 ```
 >SpringApplication
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
我们先看下判断web应用类型的方法 WebApplicationType.deduceFromClasspath()
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
判断当前什么应用做完了，那么就应该加载对应该应用需要的一些资源了，细心的同学应该已经发现了，获取初始化器和监听器的的方法都是 getSpringFactoriesInstances 只是传递的参数class不同而已，那么我们就来看看 getSpringFactoriesInstances 方法到底做了什么。
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
相关的初始化器，监听器，都加载成功过了，那么他们到底是用来干嘛的了？ spring 又默认提供了哪些初始化器 和 监听器呢？

### 1.1.2 监听器
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
可以看到 ApplicationListener 只定义了一个方法， onApplicationEvent(E event) 用于响应事件。 该接口是用于被实现对应事件的监听，那应该spring 默认的事件是哪些呢？我们先找应用上下文相关的事件。先看下面的代码：
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
|LoggingApplicationListener|ApplicationStartingEvent、ApplicationEnvironmentPreparedEvent、ApplicationPreparedEvent、ContextClosedEvent、ApplicationFailedEvent|配置对应的日志打印
|LiquibaseServiceLocatorApplicationListener|ApplicationStartingEvent|将liquibase ServiceLocator替换为与Spring引导可执行归档一起工作的版本

可以看出，spring boot 启动过程中各个阶段，都会有相关的事件需要对应的listener处理。每个listener都指负责单一的工作。虽然可能监听的事件有多个，但是处理的事情也是同一个工作。这也符合面向对象变成中`单一职责`的原则。

## 1.2. springboot 启动
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

### 1.2.1 java.awt.headless
Headless模式是系统的一种配置模式。在系统可能缺少显示设备、键盘或鼠标这些外设的情况下可以使用该模式。
Headless模式虽然不是我们愿意见到的，但事实上我们却常常需要在该模式下工作，尤其是服务器端程序开发者。因为服务器（如提供Web服务的主机）往往可能缺少前述设备，但又需要使用他们提供的功能，生成相应的数据，以提供给客户端（如浏览器所在的配有相关的显示设备、键盘和鼠标的主机）。

### 1.2.2 创建 SpringApplicationRunListeners
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
同时这里的 `this` 也把 `SpringApplication` 作为参数传递给了 正在实例化的 SpringApplicationRunListener。
#### 1 SpringApplicationRunListener
我们之前已经讲过ApplicationListener的作用呢，那么`SpringApplicationRunListener`又是干什么用的呢？我们先来看看它的代码:
 ```java
 public interface SpringApplicationRunListener {

	/**
	 * Called immediately when the run method has first started. Can be used for very
	 * early initialization.
	 */
	default void starting() {
	}

	/**
	 * Called once the environment has been prepared, but before the
	 * {@link ApplicationContext} has been created.
	 * @param environment the environment
	 */
	default void environmentPrepared(ConfigurableEnvironment environment) {
	}

	/**
	 * Called once the {@link ApplicationContext} has been created and prepared, but
	 * before sources have been loaded.
	 * @param context the application context
	 */
	default void contextPrepared(ConfigurableApplicationContext context) {
	}

	/**
	 * Called once the application context has been loaded but before it has been
	 * refreshed.
	 * @param context the application context
	 */
	default void contextLoaded(ConfigurableApplicationContext context) {
	}

	/**
	 * The context has been refreshed and the application has started but
	 * {@link CommandLineRunner CommandLineRunners} and {@link ApplicationRunner
	 * ApplicationRunners} have not been called.
	 * @param context the application context.
	 * @since 2.0.0
	 */
	default void started(ConfigurableApplicationContext context) {
	}

	/**
	 * Called immediately before the run method finishes, when the application context has
	 * been refreshed and all {@link CommandLineRunner CommandLineRunners} and
	 * {@link ApplicationRunner ApplicationRunners} have been called.
	 * @param context the application context.
	 * @since 2.0.0
	 */
	default void running(ConfigurableApplicationContext context) {
	}

	/**
	 * Called when a failure occurs when running the application.
	 * @param context the application context or {@code null} if a failure occurred before
	 * the context was created
	 * @param exception the failure
	 * @since 2.0.0
	 */
	default void failed(ConfigurableApplicationContext context, Throwable exception) {
	}

 }
 ```
 从上面的接口定义中，我们可以看到，`SpringApplicationRunListener` 总共定义了 7 方法，不同的方法在 spring boot 加载的某一个阶段需要进行调用。
 |方法|介绍|
 |--|--|--|
 |starting|`SpringApplication.run`方法第一次启动的时候，就可以立即调用该方法，可用于非常早的初始化|
 |environmentPrepared|在`environment`准备好，`application context`还未创建时|
 |contextPrepared|在`application context`创建、准备后，并在资源还未加载时调用|
 |contextLoaded|在`application context`加载后，在其刷新前调用|
 |started|在`context`刷新后和应用启动后,但是在 `CommandLineRunners` `ApplicationRunner` 调用之前|
 |running|在`application context`刷新并且 `CommandLineRunners` `ApplicationRunner` 调用后 在`SpringApplication.run`方法结束时立即调用
 |failed|在应用程序出现故障时调用|

知道了SpringApplicationRunListener的用途了后，我们再来看看它的具体实现

 ```properties
 # Run Listeners
 org.springframework.boot.SpringApplicationRunListener=\
 org.springframework.boot.context.event.EventPublishingRunListener
 ```

#### 2 EventPublishingRunListener
可以看到配置了 `EventPublishingRunListener` 是 SpringApplicationRunListener 的实现,通过命名，我们大概能明白是推送事件的运行监听器。
 ```java
 public class EventPublishingRunListener implements SpringApplicationRunListener, Ordered {

	//SpringApplication 应用
	private final SpringApplication application;

	//启动参数
	private final String[] args;

	//一个事件广播器
	private final SimpleApplicationEventMulticaster initialMulticaster;

	public EventPublishingRunListener(SpringApplication application, String[] args) {
		this.application = application;
		this.args = args;
		this.initialMulticaster = new SimpleApplicationEventMulticaster();
		//将之前SpringApplication初始化阶段的listener都让SimpleApplicationEventMulticaster来做通知
		for (ApplicationListener<?> listener : application.getListeners()) {
			this.initialMulticaster.addApplicationListener(listener);
		}
	}

	@Override
	public int getOrder() {
		return 0;
	}

	//推送 ApplicationStartingEvent 应用正在启动的事件
	@Override
	public void starting() {
		this.initialMulticaster.multicastEvent(new ApplicationStartingEvent(this.application, this.args));
	}

	//推送 ApplicationEnvironmentPreparedEvent 已环境准备的事件
	@Override
	public void environmentPrepared(ConfigurableEnvironment environment) {
		this.initialMulticaster
				.multicastEvent(new ApplicationEnvironmentPreparedEvent(this.application, this.args, environment));
	}

	//推送 ApplicationContextInitializedEvent 应用上下文初始化事件
	@Override
	public void contextPrepared(ConfigurableApplicationContext context) {
		this.initialMulticaster
				.multicastEvent(new ApplicationContextInitializedEvent(this.application, this.args, context));
	}

	//推送 ApplicationPreparedEvent 应用已准备的事件
	@Override
	public void contextLoaded(ConfigurableApplicationContext context) {
		//从所有 ApplicationListener 中找到同时实现了 ApplicationContextAware 的接口
		//并将这些监听器，加到 应用上下文当中去的 应用监听器去
		for (ApplicationListener<?> listener : this.application.getListeners()) {
			if (listener instanceof ApplicationContextAware) {
				((ApplicationContextAware) listener).setApplicationContext(context);
			}
			context.addApplicationListener(listener);
		}
		//推送 ApplicationPreparedEvent 事件
		this.initialMulticaster.multicastEvent(new ApplicationPreparedEvent(this.application, this.args, context));
	}

	//推送 ApplicationStartedEvent 应用启动的事件，但是 ApplicationRunner 和 CommandLineRunner 还没有被调用时
	@Override
	public void started(ConfigurableApplicationContext context) {
		context.publishEvent(new ApplicationStartedEvent(this.application, this.args, context));
	}

	//推送 ApplicationReadyEvent 应用准备就绪的事件
	@Override
	public void running(ConfigurableApplicationContext context) {
		context.publishEvent(new ApplicationReadyEvent(this.application, this.args, context));
	}

	//推送 ApplicationFailedEvent 失败事件
	@Override
	public void failed(ConfigurableApplicationContext context, Throwable exception) {
		ApplicationFailedEvent event = new ApplicationFailedEvent(this.application, this.args, context, exception);
		if (context != null && context.isActive()) {
			// Listeners have been registered to the application context so we should
			// use it at this point if we can
			context.publishEvent(event);
		}
		else {
			// An inactive context may not have a multicaster so we use our multicaster to
			// call all of the context's listeners instead
			if (context instanceof AbstractApplicationContext) {
				for (ApplicationListener<?> listener : ((AbstractApplicationContext) context)
						.getApplicationListeners()) {
					this.initialMulticaster.addApplicationListener(listener);
				}
			}
			this.initialMulticaster.setErrorHandler(new LoggingErrorHandler());
			this.initialMulticaster.multicastEvent(event);
		}
	}

	private static class LoggingErrorHandler implements ErrorHandler {

		private static final Log logger = LogFactory.getLog(EventPublishingRunListener.class);

		@Override
		public void handleError(Throwable throwable) {
			logger.warn("Error calling ApplicationEventListener", throwable);
		}

	}

 }
 ```
 
在上面的代码中，我们可以看到 ApplicationListener 监听的其他事件，也就是说 `SpringApplicationRunListener` 是贯穿整个Spring Boot的启动周期的， Spring Boot 每个阶段的变更，都是由它发出对应的通知，而`SpringApplicationRunListener` 只是提供将 Spring Boot 启动周期的各个阶段，定义出对应的方法，并在内部封装成对应的事件，由一个`SimpleApplicationEventMulticaster`类型的变量 `initialMulticaster` 事件广播器 再发出事件通知。接下来我们看看`SimpleApplicationEventMulticaster`的代码，看一下它是如何将不同的事件，准确的推送给对应的listner的。

#### 3 SimpleApplicationEventMulticaster
`SimpleApplicationEventMulticaster` 继承了 `AbstractApplicationEventMulticaster` 其作用是广播所有spring事件给注册的 listner ，监听器需要自己忽略不感兴趣的事件。
我们来看下，他的广播方法。
 ```java
 	public void multicastEvent(ApplicationEvent event) {
		//广播事件
		multicastEvent(event, resolveDefaultEventType(event));
	}
	public void multicastEvent(final ApplicationEvent event, @Nullable ResolvableType eventType) {
		//转换类型
		ResolvableType type = (eventType != null ? eventType : resolveDefaultEventType(event));
		//获取线程池执行器
		Executor executor = getTaskExecutor();
		//获取对应的 listener
		for (ApplicationListener<?> listener : getApplicationListeners(event, type)) {
			//若启动了线程池，则由线程池执行调用 listener
			if (executor != null) {
				executor.execute(() -> invokeListener(listener, event));
			}
			else {
				//没有线程池，直接调用 listener
				invokeListener(listener, event);
			}
		}
	}
 ```
其中有3个比较重要的方法，
1. resolveDefaultEventType(event),将 ApplicationEvent 进行包装
1. getApplicationListeners(event, type) 获取对应的 listener
1. invokeListener(listener, event)调用listener，完成事件通知

#### 3.1 方法 resolveDefaultEventType(event)
首先我们看下 resolveDefaultEventType(event) 方法：
 ```java
 	private ResolvableType resolveDefaultEventType(ApplicationEvent event) {
		return ResolvableType.forInstance(event);
	}
 ```
 ```java
 	public static ResolvableType forInstance(Object instance) {
		Assert.notNull(instance, "Instance must not be null");
		if (instance instanceof ResolvableTypeProvider) {
			ResolvableType type = ((ResolvableTypeProvider) instance).getResolvableType();
			if (type != null) {
				return type;
			}
		}
		return ResolvableType.forClass(instance.getClass());
	}
 ```
`ResolvableType` 是一个 spring-core 中的一个基础类，主要用来做一些类的包装。有兴趣后续的同学可以去研究研究。

#### 3.1 方法 getApplicationListeners(event, type)
这个方法应该是事件通知比较重要的一步了，它需要筛选出监听对应的事件的listener，其中加入了很多相关的逻辑判断，还有本地的concurrentHashMap作为缓存，我们主要看的是下面这个方法：
 ```java
	protected boolean supportsEvent(
			ApplicationListener<?> listener, ResolvableType eventType, @Nullable Class<?> sourceType) {

		//将 listener 包装成一个 一般的 监听适配器
		//这里需要注意的是，先判断 listener 如果实现了 GenericApplicationListener，就自己提供支持事件类型方法
		//没有，就封装为 GenericApplicationListenerAdapter 由适配器来做适配事件工作
		GenericApplicationListener smartListener = (listener instanceof GenericApplicationListener ?
				(GenericApplicationListener) listener : new GenericApplicationListenerAdapter(listener));
		//由监听适配器，判断是否支持该 事件
		return (smartListener.supportsEventType(eventType) && smartListener.supportsSourceType(sourceType));
	}
 ```
内部的一些判断逻辑这里就不做详细说明了，有兴趣的同学可以去看看。(主要是通过查找ApplicationListener的参数类型做的判断)


### 1.2.3 starting 事件
接下来就是开始启动了，`listeners.starting()`, 该方法，最终由 `EventPublishingRunListner` 封装对应的事件，并委托给 `SimpleApplicationEventMulticaster` 广播出去。
 ```java
 this.initialMulticaster.multicastEvent(new ApplicationStartingEvent(this.application, this.args));
 ```
在之间介绍 `ApplicationEvent` 和 `ApplicationListener` 时，我们已经了解到了，监听 `ApplicationStartingEvent` 事件的两个 Listener 为：
* LoggingApplicationListener
* LiquibaseServiceLocatorApplicationListener
我们先来看看 `LoggingApplicationListener`
 ```java
 @Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ApplicationStartingEvent) {
			onApplicationStartingEvent((ApplicationStartingEvent) event);
		}
		else if (event instanceof ApplicationEnvironmentPreparedEvent) {
			onApplicationEnvironmentPreparedEvent((ApplicationEnvironmentPreparedEvent) event);
		}
		else if (event instanceof ApplicationPreparedEvent) {
			onApplicationPreparedEvent((ApplicationPreparedEvent) event);
		}
		else if (event instanceof ContextClosedEvent
				&& ((ContextClosedEvent) event).getApplicationContext().getParent() == null) {
			onContextClosedEvent();
		}
		else if (event instanceof ApplicationFailedEvent) {
			onApplicationFailedEvent();
		}
	}

	private void onApplicationStartingEvent(ApplicationStartingEvent event) {
		this.loggingSystem = LoggingSystem.get(event.getSpringApplication().getClassLoader());
		this.loggingSystem.beforeInitialize();
	}
 ```
