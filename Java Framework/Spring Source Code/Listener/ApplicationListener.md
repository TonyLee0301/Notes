# ApplicationListener 监听器
ApplicationListener 基于基础的 java.util.EventListener 接口用于观察者模式；主要为用于spring内部的事件通知。
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
可以看到 ApplicationListener 只定义了一个方法， onApplicationEvent(E event) 用于响应事件。 该接口是用于被实现对应事件的监听。

ApplicatioinEvent 是所有 `spring` 应用事件的抽象类, 它需要被各事件扩展实现。
 ```java
public abstract class ApplicationEvent extends EventObject {

	/** use serialVersionUID from Spring 1.2 for interoperability. */
	private static final long serialVersionUID = 7099057708183571937L;

	/** System time when the event happened. */
	private final long timestamp;


	/**
	 * Create a new {@code ApplicationEvent}.
	 * @param source the object on which the event initially occurred or with
	 * which the event is associated (never {@code null})
	 */
	public ApplicationEvent(Object source) {
		super(source);
		this.timestamp = System.currentTimeMillis();
	}


	/**
	 * Return the system time in milliseconds when the event occurred.
	 */
	public final long getTimestamp() {
		return this.timestamp;
	}

}
 ```
## ApplicationListener 模式实现
我们已经知道了，ApplicationListener 是观察者模式中的观察者，ApplicationEvent 是主题推送的事件。
我们先来看看，ApplicationListener 的一些默认实现和它的一些常见事件。

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

从上面的表格中，我们可以看到，相关的一些监听器，和其监听的一些事件。

### GenericApplicationListener
我们先来看另外一个接口 GenericApplicationListener
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
可以看到该接口定义了两个方法，判断该 `listener` 支持的 事件类型 和 资源类型。
