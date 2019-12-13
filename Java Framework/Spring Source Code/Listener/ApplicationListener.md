# ApplicationListener 监听器
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

## ApplicationEvent