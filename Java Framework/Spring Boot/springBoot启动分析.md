1. SpringBoot 启动流程源码分析
> 该spring boot 的源码是在 2.2.x 的基础上分析的。
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
