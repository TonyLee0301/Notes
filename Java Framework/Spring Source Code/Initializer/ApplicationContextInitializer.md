# ApplicationContextInitializer 初始化器
下面我们来看看 ApplicationContextInitializer 监听器都可以干什么，在什么时候使用。
 ```java
  /**
  * 在 ConfigurableApplicationContext refresh() 调用之前,初始化 Spring ConfigurableApplicationContext 的回调接口
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
可以看出 ApplicationContextInitializer 的说明如其命名一样，用于初始化Spring的应用上下文。

## springboot ApplicationInitializer的默认实现
查找META-INFO下的 spring.factories 文件，找到 org.springframework.context.ApplicationContextInitializer 的配置
在spring-boot中我们先看下常用的个配置
 ```properties
 org.springframework.context.ApplicationContextInitializer=\
 org.springframework.boot.context.ConfigurationWarningsApplicationContextInitializer,\
 org.springframework.boot.context.ContextIdApplicationContextInitializer,\
 org.springframework.boot.context.config.DelegatingApplicationContextInitializer,\
 org.springframework.boot.rsocket.context.RSocketPortInfoApplicationContextInitializer,\
 org.springframework.boot.web.context.ServerPortInfoApplicationContextInitializer
 ```
在spring-boot-autoconfigure
 ```properties
 org.springframework.context.ApplicationContextInitializer=\
 org.springframework.boot.autoconfigure.SharedMetadataReaderFactoryContextInitializer,\
 org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener
 ```
像spring-boot-actuator，spring-boot-devtools 等其他相关的初始化器，咱们就先暂时不做介绍。

这么多相关应用上下文初始化器，具体是用来干什么的了？
|初始化器|介绍|
|---|---|
ConfigurationWarningsApplicationContextInitializer|用于报告一些常见的错误配置的错误信息
ContextIdApplicationContextInitializer|用于创建应用程序上下文 id
|DelegatingApplicationContextInitializer|用于代表执行环境 context.initializer.classes 下指定的初始化器，其实就是spring boot 提供给使用者的一个可以自己实现 初始化器 的一个方式。
RSocketPortInfoApplicationContextInitializer| 将 RSocket 响应式编程的端口号 写到 Environment 环境属性当中，这样 local.rsocket.server.port 就可以直接使用 @Value 注入 或通过 Environment 访问
ServerPortInfoApplicationContextInitializer| 将 webServer 的端口  写到 Environment 环境属性中，这样 local.server.port 就可以直接使用 @Value 注入 或者 通过 Environment 访问，同时可以设置 ServerNameSpace 的端口
SharedMetadataReaderFactoryContextInitializer|创建一个 spring boot 和 ConfigurationClassPostProcessor 共用的 CachingMetadataReaderFactory 具体实现为 CachingMetadataReaderFactoryPostProcessor
ConditionEvaluationReportLoggingListener| ConditionEvaluationReport写入日志 

查看着几个常用的 Initializer 比如 SharedMetadataReaderFactoryContextInitializer 中的 initialize 方法
 ```java
 	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		applicationContext.addBeanFactoryPostProcessor(new CachingMetadataReaderFactoryPostProcessor());
	}
 ```
我们可以看到 它们的作用更多的是做一些准备工作,例如 ContextIdApplicationContextInitializer 生成 context, ServerPortInfoApplicationContextInitializer 将 WebServerInitializedEvent 时间的监听器(也是自己)添加到 应用上下文中 ApplicationListner中， SharedMetadataReaderFactoryContextInitializer 将 CachingMetadataReaderFactoryPostProcessor 添加到 应用上下文中 BeanFactoryPostProcessor 中。