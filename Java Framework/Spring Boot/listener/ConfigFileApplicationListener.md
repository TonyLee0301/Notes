# ConfigFileApplicationListener
&emsp;&emsp; ConfigFileApplicationListener 分别实现了3个接口，EnvironmentPostProcessor , SmartApplicationListener , Ordered;
其主要用途：
* 配置 config 的一些文件；

 ```java
public class ConfigFileApplicationListener implements EnvironmentPostProcessor, SmartApplicationListener, Ordered{...}
 ```
接下来，我们来看看，它说监听的事件是哪些
 ```java

    @Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		return ApplicationEnvironmentPreparedEvent.class.isAssignableFrom(eventType)
				|| ApplicationPreparedEvent.class.isAssignableFrom(eventType);
	}

    @Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ApplicationEnvironmentPreparedEvent) {
			onApplicationEnvironmentPreparedEvent((ApplicationEnvironmentPreparedEvent) event);
		}
		if (event instanceof ApplicationPreparedEvent) {
			onApplicationPreparedEvent(event);
		}
	}
 ```
&emsp;&emsp;可以看到，监听的两个事件为：ApplicationEnvironmentPreparedEvent 和 ApplicationPreparedEvent

## ApplicationEnvironmentPreparedEvent 事件
我们先来看看 ApplicationEnvironmentPreparedEvent 事件
 ```java
    private void onApplicationEnvironmentPreparedEvent(ApplicationEnvironmentPreparedEvent event) {
		//加载 EnvironmentPostProcessor
		List<EnvironmentPostProcessor> postProcessors = loadPostProcessors();
		//configFile 也是一个 EnvironmentPostProcessor ，因此把自己也加进来
		postProcessors.add(this);
		//把 postProcessors 排序
		AnnotationAwareOrderComparator.sort(postProcessors);
		//轮询 并执行 postProcessor 的 postProcessEnvironment方法
		for (EnvironmentPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessEnvironment(event.getEnvironment(), event.getSpringApplication());
		}
	}
 ```
&emsp;&emsp;这里的 `loadPostProcessors` 同样适用的 spring 的 spi 机制，通过 META-INFO/spring.factories 获取对应的实现。在spring.factories 中找到
 ```properties
org.springframework.boot.env.EnvironmentPostProcessor=\
org.springframework.boot.cloud.CloudFoundryVcapEnvironmentPostProcessor,\
org.springframework.boot.env.SpringApplicationJsonEnvironmentPostProcessor,\
org.springframework.boot.env.SystemEnvironmentPropertySourceEnvironmentPostProcessor,\
org.springframework.boot.reactor.DebugAgentEnvironmentPostProcessor
 ```
从之前 获取 postProcessor ，首先是判断是否 postProcessor 的优先级
我们可以大概看一下排序规则
 ```java
 	@Override
	public int compare(@Nullable Object o1, @Nullable Object o2) {
		return doCompare(o1, o2, null);
	}

	private int doCompare(@Nullable Object o1, @Nullable Object o2, @Nullable OrderSourceProvider sourceProvider) {
		boolean p1 = (o1 instanceof PriorityOrdered);
		boolean p2 = (o2 instanceof PriorityOrdered);
		if (p1 && !p2) {
			return -1;
		}
		else if (p2 && !p1) {
			return 1;
		}

		int i1 = getOrder(o1, sourceProvider);
		int i2 = getOrder(o2, sourceProvider);
		return Integer.compare(i1, i2);
	}
	protected int getOrder(@Nullable Object obj) {
		if (obj != null) {
			Integer order = findOrder(obj);
			if (order != null) {
				return order;
			}
		}
		return Ordered.LOWEST_PRECEDENCE;
	}
 ```
可以看出排序规则为，实现 PriorityOrdered 的优先级越高，如果相同者取每个后置处理器中的 order 变量来做对比。
也就是说，我们这几个类的执行顺序是：

1. SystemEnvironmentPropertySourceEnvironmentPostProcessor
 ```java
 public static final int DEFAULT_ORDER = SpringApplicationJsonEnvironmentPostProcessor.DEFAULT_ORDER - 1;
 ```
&emsp;&emsp; 将原本的 PropertySource 转换成 SystemEnvironmentPropertySource 并存放到 environment 的 properties
2. SpringApplicationJsonEnvironmentPostProcessor
 ```java
 public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 5;
 ```
&emsp;&emsp;查找环境中的 spring.application.json 配置数据，该配置只能是 json 格式，解析该数据，并将 json 转换成 spring 配置文件的相关配置。
例如:
 ```json
	{
		"a":{
			"a1" : "1"
		}
		"b":"b"
	}
	//则转换成
	a.a1 = 1
	b = b
 ```
首先会去查找，当前 环境中 是否存在几个配置，
如果是 servlet 的环境，存在 `jndiProperties`,`servletContextInitParams`,`servletConfigInitParams` 配置, 则添加到按该顺序查找到的第一个配置之前。
如果不是，查看 环境是否存在 `systemProperties` 配置。
* 如果存在 添加到 `systemProperties` 配置前；
* 不存在，则添加到 把json配置，添加到第一位；


3. CloudFoundryVcapEnvironmentPostProcessor
主要是用于支持 spring cloud 将相关数据转换成本地环境好理解的数据；

4. ConfigFileApplicationListener
 ```java
 public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;
 ```
上面3个我们可以看到，都是做一些配置数据的转换，或者是加载一些jvm环境配置。
 ```java
    @Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		addPropertySources(environment, application.getResourceLoader());
	}
	/**
	 * Add config file property sources to the specified environment.
	 * @param environment the environment to add source to
	 * @param resourceLoader the resource loader
	 * @see #addPostProcessors(ConfigurableApplicationContext)
	 */
	protected void addPropertySources(ConfigurableEnvironment environment, ResourceLoader resourceLoader) {
		RandomValuePropertySource.addToEnvironment(environment);
		new Loader(environment, resourceLoader).load();
	}
 ```
addPropertySources 方法主要的处理逻辑都到了 Loader 的 load 方法，下面我们来跟踪一下相关的方法：
 ```java
 Loader(ConfigurableEnvironment environment, ResourceLoader resourceLoader) {
			this.environment = environment;
			//针对这个 环境 创建一个 PropertySourcesPlaceholdersResolver
			this.placeholdersResolver = new PropertySourcesPlaceholdersResolver(this.environment);
			//设置 resourceLoader
			this.resourceLoader = (resourceLoader != null) ? resourceLoader : new DefaultResourceLoader();
			//SPI 机制加载 PropertySourceLoader 的实现
			//org.springframework.boot.env.PropertySourceLoader=\
			//org.springframework.boot.env.PropertiesPropertySourceLoader,\
			//org.springframework.boot.env.YamlPropertySourceLoader
			this.propertySourceLoaders = SpringFactoriesLoader.loadFactories(PropertySourceLoader.class,
					getClass().getClassLoader());
		}
 ```
&emsp;&emsp;我们先用 `load` 方法中的 `FilteredPropertySource.apply` 方法
 ```java
 static void apply(ConfigurableEnvironment environment, String propertySourceName, Set<String> filteredProperties,
			Consumer<PropertySource<?>> operation) {
		//获取环境中配置信息
		MutablePropertySources propertySources = environment.getPropertySources();
		//获取名为 propertySourceName 的配置信息
		PropertySource<?> original = propertySources.get(propertySourceName);
		if (original == null) {
			operation.accept(null);
			return;
		}
		// 将原本的propertySource 替换成可过滤的 FilteredPropertySource
		propertySources.replace(propertySourceName, new FilteredPropertySource(original, filteredProperties));
		try {
			//执行 获取到的 propertySourceName 配置信息
			operation.accept(original);
		}
		finally {
			//再次替换
			propertySources.replace(propertySourceName, original);
		}
	}
 ```
&emsp;&emsp;回头我们在看看 load 方法中实现的 consumer
 ```java
	∂
 ```

