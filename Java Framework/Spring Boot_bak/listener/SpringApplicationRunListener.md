
<!-- @import "[TOC]" {cmd="toc" depthFrom=1 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->

- [SpringApplicationRunListener](#springapplicationrunlistener)
  - [1 EventPublishingRunListener](#1-eventpublishingrunlistener)
  - [2 SimpleApplicationEventMulticaster](#2-simpleapplicationeventmulticaster)
      - [2.1 方法 resolveDefaultEventType(event)](#21-方法-resolvedefaulteventtypeevent)
      - [2.2 方法 getApplicationListeners(event, type)](#22-方法-getapplicationlistenersevent-type)
  - [3 spring boot 启动过程中的事件](#3-spring-boot-启动过程中的事件)
    - [ starting 事件 ](#div-idstarting-starting-事件-div)
    - [ environmentPrepared 事件 ](#div-idenvironmentprepared-environmentprepared-事件-div)
    - [ contextPrepared 事件 ](#div-idcontextprepared-contextprepared-事件-div)
    - [ contextLoaded 事件 ](#div-idcontextloaded-contextloaded-事件-div)
    - [ started 事件 ](#div-idstarted-started-事件-div)
    - [ running 事件 ](#div-idrunning-running-事件-div)
    - [ failed 事件 ](#div-idfailed-failed-事件-div)

<!-- /code_chunk_output -->

# SpringApplicationRunListener
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
 |<a href="#starting">starting</a>|`SpringApplication.run`方法第一次启动的时候，就可以立即调用该方法，可用于非常早的初始化|
 |<a href="#environmentPrepared">environmentPrepared</a>|在`environment`准备好，`application context`还未创建时|
 |<a href="#contextPrepared">contextPrepared</a>|在`application context`创建、准备后，并在资源还未加载时调用|
 |<a href="#contextLoaded">contextLoaded</a>|在`application context`加载后，在其刷新前调用|
 |<a href="#started">started</a>|在`context`刷新后和应用启动后,但是在 `CommandLineRunners` `ApplicationRunner` 调用之前|
 |<a href="#running">running</a>|在`application context`刷新并且 `CommandLineRunners` `ApplicationRunner` 调用后 在`SpringApplication.run`方法结束时立即调用
 |<a href="#failed">failed</a>|在应用程序出现故障时调用|

知道了SpringApplicationRunListener的用途了后，我们再来看看它的具体实现

 ```properties
 # Run Listeners
 org.springframework.boot.SpringApplicationRunListener=\
 org.springframework.boot.context.event.EventPublishingRunListener
 ```

## 1 EventPublishingRunListener
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

## 2 SimpleApplicationEventMulticaster
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

#### 2.1 方法 resolveDefaultEventType(event)
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

#### 2.2 方法 getApplicationListeners(event, type)
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
内部的一些判断逻辑这里就不做详细说明了，有兴趣的同学可以去看看。(主要是通过查找ApplicationListener的参数类型做的判断)，用到了**适配器模式**。

## 3 spring boot 启动过程中的事件
在之间介绍 `ApplicationEvent` 和 `ApplicationListener` 时,我们已经了解到了一些相关的
### <div id="starting"> starting 事件 </div>
监听 `ApplicationStartingEvent` 事件的两个 Listener 为：
* [LoggingApplicationListener](../listener/LoggingApplicationListener.md)
* LiquibaseServiceLocatorApplicationListener
 
### <div id="environmentPrepared"> environmentPrepared 事件 </div>

### <div id="contextPrepared"> contextPrepared 事件 </div>


### <div id="contextLoaded"> contextLoaded 事件 </div>

### <div id="started"> started 事件 </div>

### <div id="running"> running 事件 </div>

### <div id="failed"> failed 事件 </div>

