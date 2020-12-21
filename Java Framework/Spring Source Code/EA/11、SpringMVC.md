# 11 SpringMVC

<!-- @import "[TOC]" {cmd="toc" depthFrom=1 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->

- [11 SpringMVC](#11-springmvc)
  - [11.1 SpringMVC快速体验](#111-springmvc快速体验)
  - [11.2 ContextLoaderListener](#112-contextloaderlistener)
    - [11.2.1 ServletContextListener 的使用](#1121-servletcontextlistener-的使用)
    - [11.2.2 Spring 中 ContextLoaderListener](#1122-spring-中-contextloaderlistener)
  - [11.3 DispatcherServlet](#113-dispatcherservlet)
    - [1. 初始化阶段](#1-初始化阶段)
    - [2. 运行阶段](#2-运行阶段)
    - [3. 销毁阶段](#3-销毁阶段)
    - [11.3.1 servlet 的使用](#1131-servlet-的使用)
    - [11.3.2 DispatcherServlet 的初始化](#1132-dispatcherservlet-的初始化)
    - [11.3.3 WebApplicationContext 的初始化](#1133-webapplicationcontext-的初始化)
      - [1. 寻找或创建对应的 WebApplicationContext 实例](#1-寻找或创建对应的-webapplicationcontext-实例)
    - [2. configureAndRefreshWebApplicationContext](#2-configureandrefreshwebapplicationcontext)
    - [3. 刷新](#3-刷新)
      - [1. 初始化 MultipartResolver。](#1-初始化-multipartresolver)
      - [2. 初始化 LocaleResolver。](#2-初始化-localeresolver)
      - [4. 初始化 HandlerMappings](#4-初始化-handlermappings)
      - [5. 初始化 HandlerAdapters](#5-初始化-handleradapters)
      - [6. 初始化 HanlderExceptionResolvers](#6-初始化-hanlderexceptionresolvers)
      - [7. 初始化 RequestToViewNameTranslator](#7-初始化-requesttoviewnametranslator)

<!-- /code_chunk_output -->

&emsp;&emsp;Spring框架提供了构建Web应用程序的全功能MVC模块。通常策略接口，Spring 框架是高度可配置，而且支持多种试图技术、例如JavaServer Pages(JSP)、Velocity、Tiles、iText、POI。SpringMVC框架并不知道使用的试图，所以不会强迫您只使用JSP技术。SpringMVC分离了控制器、模型对象、分派器以及处理程序对象的角色，这种分离让它们更容易进行定制。
&emsp;&emsp;Spring的MVC是基于Servlet功能实现的，通过实现Servlet接口的DispatcherServlet来封装其核心功能，通过将请求分派给处理程序，同样带有可配置的处理程序映射、视图解析、本地语言、主题解析、以及上载文件支持。默认的处理程序是非常简单的Controller接口，只有一个方法 ModelAndView handleRequest(request,response)。Spring提供了一个控制器层次结构、可以派生之类。如果应用程序需要处理用户输入表单，那么可以继承 AbstractFormController。如果需要把多页输入处理到一个表单，那么可以继承AbstractWizardFormController。
&emsp;&emsp;对于 SpringMVC或者其他比较成熟的MVC框架而言，解决问题无外乎一下绩点。
* 将Web页面的请求传给服务器。
* 根据不同的请求处理不同的逻辑单元。
* 返回处理结果数据并跳转至响应的页面。
&emsp;&emsp;我们首先通过一个简单示例类快速回顾 SpringMVC 的使用。

## 11.1 SpringMVC快速体验
&emsp;&emsp;SpringMVC 的配置用过的同学应该都知道，我们这里只取比较重要的几个点来举例说明。
1. 配置web.xml
&emsp;&emsp;一个Web中可以没有web.xml文件，也就是说，web.xml文件并不是Web工程必须的。web.xml文件用来初始化配置信息，比如Welcome页面、servlet、servlet-mapping、filter、listener、启动加载级别等。但是，SpringMVC的实现原理是通过servlet拦截所有URL来达到控制的目的，所以web.xml的配置是必须的。
 ```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app id="webappId" version="2.4" xmlns="http://java.sun.com/xml/ns/j2ee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd">
    <!-- 使用ContextLoaderListener 配置时，需要告诉它Spring配置文件的位置-->
    <context-param>
        <param-name>contextConfigLocation</param-name>
        <param-value>classpath:applicationContext.xml</param-value>
    </context-param>

    <!-- SpringMVC的前端控制器 -->
    <!-- 当 DispatcherServlet 载入后，它将一个XML文件中载入 Spring 的应用上下文，该 XML 文件的名字取决于 <servlet-name> -->
    <!-- 这里 DispatcherServlet 将视图从一个叫做 Springmvc-servlet.xml 的文件中载入上下文，其默认位于 WEB-INF 目录下 -->
    <servlet>
        <servlet-name>Springmvc</servlet-name>
        <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
        <load-on-startup>1</load-on-startup>
    </servlet>
    <servlet-mapping>
        <servlet-name>Springmvc</servlet-name>
        <url-pattern>*.html</url-pattern>
    </servlet-mapping>
    <!-- 配置上下文载入器 -->
    <!-- 上下文载入器 载入除 DispatcherServlet 载入的配置文件之外的其他上下文配置文件 -->
    <!-- 最常用的上下文载入器时一个 Servlet 监听器，其名称为 ContextLoaderListener -->
    <listener>
        <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
    </listener>

</web-app>
 ```
&emsp;&emsp;Spring MVC 之所以必须要配置 web.xml，其实最关键的是要配置两个地方。
* contextConfigLoacation:Spring的核心就是配置文件，可以说配置文件是 Spring 中必不可少的东西，而这个参数就是使Web 于 Spring 配置文件相结合的一个关键配置。
* DispatcherServlet：包含了 SpringMVC 的请求逻辑，Spring使用此类拦截 Web 请求并进行相关的逻辑处理。

2. 配置 Spring 配置文件 applicationContext.xml
 ```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

    <bean id="viewResolver" class="org.springframework.web.servlet.view.InternalResourceViewResolver">
        <property name="prefix" value="/WEB-INF/jsp/"/>
        <property name="suffix" value=".jsp"/>
    </bean>
</beans>
 ```
&emsp;&emsp;InternalResourceViewResolver 是一个辅助 bean， 会在 ModelAndView 返回的视图名前加上 prefix 指定的前缀，再在后面加上 suffix 指定的后缀，例如：由于 XXController 返回的ModelAndView 中的视图名是 testview，故该视图解析器将在 /WEB-INF/jsp/testview.jsp 处查找视图。
3. 创建model
4. 创建controller
5. 创建视图文件 **.jsp
6. 创建Servlet配置文件 Spring-servlet.xml
&emsp;&emsp;即创建对应的 controller 映射。 因为 SpringMVC 是基于 Servlet 的实现，所以在Web启动的时候，服务器会首先尝试加载对应于 Servlet 的配置文件，而为了让项目更加模块化，我们通常将 Web 部分的配置文件存放于此配置文件中。
&emsp;&emsp;至此，已经完成了 SpringMVC 的创建，启动服务器，输入网址 即可看到效果。

## 11.2 ContextLoaderListener
&emsp;&emsp;对于 SpringMVC 功能实现的分析，我们首先从 web.xml 开始，在 web.xml 文件中我们首先配置的就是 ContextLoaderListener， 那么它所提供的功能有哪些，又是如何实现的呢？
&emsp;&emsp;当使用编程方式的时候我们可以直接将 Spring 配置信息作为参数传入 Spring 容器中，如：
ApplicationContext ac = new ClassPathXmlApplicationContext("applicationContext.xml");
&emsp;&emsp;但是在 Web 下，我们需要更多的是与 Web 环境相互结合，通常的方法是将路径以 context-param 方式注册并使用 ContextLoaderListener 进行监听读取。
&emsp;&emsp;ContextLoaderListener 的作用就是启动 Web 容器时，自动装配 ApplicationContext 的配置信息。因为它实现了 ServletContextListener 这个接口，在 web.xml 配置这个监听器，启动容器时，就会默认执行它实现的方法，使用 ServletContextListener 接口，开发者能够在为客户端请求提供服务之前向 ServletContext 中添加任意的对象。这个对象在 ServletContext 启动的时候被初始化，然后在 ServletContext 整个运行期间都是可见的。
&emsp;&emsp;每一个 Web 应用都有一个 ServletContext 与之相关联。ServletContext 对象在应用启动时被创建，在应用关闭的时候被销毁。ServletContext 在全局范围内有效，类似于应用中的一个全局变量。
&emsp;&emsp;在 ServletContextListener 中核心逻辑便是初始化 WebApplicationContext 实例并存放至 ServletContext 中。

### 11.2.1 ServletContextListener 的使用
&emsp;&emsp;正式分析代码前我们同样还是先了解 ServletContextListener 的使用。
1. 创建自定义 ServletContextListener
&emsp;&emsp;首先我们创建 ServletContextListener，目标是在系统启动时添加自定义属性，以便于在全局范围内可以随时调用。系统启动的时候会调用 ServletContextListener 实现类的 contextInitialized 方法，所以需要在这个方法中实现我们的初始化逻辑。
 ```java
 public class MyDataContextListener implements ServletContextListener {
     private ServletContext context = null;

     public MyDataContextListener(){
     }

     //该方法在ServletContext启动后被调用，并准备好处理客户端请求
     public void contextInitialized(ServletContextEvent event){
         this.context = event.getServletContext();
         //实现自己的逻辑并将结果记录在属性中
         context = setAttribute("mydata", "this is myData");
     }
     
     //该方法在 ServletContext 将要关闭的时候调用
     public void contextDestroyed(ServletContextEvent event){
         this.context = null;
     }
 }
 ```
2. 注册监听器
&emsp;&emsp;在web.xml文件中需要注册自定义的监听器。
 ```xml
 <listener>com.test.MyDataContextListener</listener>
 ```
3. 测试
&emsp;&emsp;一旦 Web 应用启动的时候，我们就能在任意的 Servlet 或者 JSP 中通过下面的方式获取我们初始化参数，如下：
String myData = (String)getServletContext().getAttribute("myData");

### 11.2.2 Spring 中 ContextLoaderListener
&emsp;&emsp;分析了 ServletContextListener 的使用方式后再来分析 Spring 中的 ContextLoaderListener 的实现就容易理解多， 虽然 ContextLoaderListener 实现的逻辑要复杂得多，但是大致的套路还是万变不离其宗。
&emsp;&emsp;ServletContext 启动之后会调用 ServletContextListener 的 contextinitialized 方法，那么，我们就从这个函数开始进行分析。
 ```java
	/**
	 * Initialize the root web application context.
	 */
	@Override
	public void contextInitialized(ServletContextEvent event) {
		initWebApplicationContext(event.getServletContext());
	}
 ```
&emsp;&emsp;这里涉及到一个常用类 WebApplicationContext： 在 Web 应用中，我们会用到 WebApplicationContext， WebApplicationContext 操作 继承自 ApplicationContext ，在 ApplicationContext 的基础上又追加了一些特定于 Web 的操作及属性，非常类似于我们通过编码方式使用 Spring 时使用的 ClassPathXmlApplicationContext 类提供的功能。继续跟踪代码：
 ```java
    /**
	 * Initialize Spring's web application context for the given servlet context,
	 * using the application context provided at construction time, or creating a new one
	 * according to the "{@link #CONTEXT_CLASS_PARAM contextClass}" and
	 * "{@link #CONFIG_LOCATION_PARAM contextConfigLocation}" context-params.
	 * @param servletContext current servlet context
	 * @return the new WebApplicationContext
	 * @see #ContextLoader(WebApplicationContext)
	 * @see #CONTEXT_CLASS_PARAM
	 * @see #CONFIG_LOCATION_PARAM
	 */
	public WebApplicationContext initWebApplicationContext(ServletContext servletContext) {
		// web.xml 中存在多次 ContextLoader 定义
		if (servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE) != null) {
			throw new IllegalStateException(
					"Cannot initialize context because there is already a root application context present - " +
					"check whether you have multiple ContextLoader* definitions in your web.xml!");
		}

		servletContext.log("Initializing Spring root WebApplicationContext");
		Log logger = LogFactory.getLog(ContextLoader.class);
		if (logger.isInfoEnabled()) {
			logger.info("Root WebApplicationContext: initialization started");
		}
		long startTime = System.currentTimeMillis();

		try {
			// Store context in local instance variable, to guarantee that
			// it is available on ServletContext shutdown.
			if (this.context == null) {
				//初始化 context
				this.context = createWebApplicationContext(servletContext);
			}
			if (this.context instanceof ConfigurableWebApplicationContext) {
				ConfigurableWebApplicationContext cwac = (ConfigurableWebApplicationContext) this.context;
				if (!cwac.isActive()) {
					// The context has not yet been refreshed -> provide services such as
					// setting the parent context, setting the application context id, etc
					if (cwac.getParent() == null) {
						// The context instance was injected without an explicit parent ->
						// determine parent for root web application context, if any.
						ApplicationContext parent = loadParentContext(servletContext);
						cwac.setParent(parent);
					}
					configureAndRefreshWebApplicationContext(cwac, servletContext);
				}
			}
			//记录在 servletContext 中
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.context);

			ClassLoader ccl = Thread.currentThread().getContextClassLoader();
			if (ccl == ContextLoader.class.getClassLoader()) {
				currentContext = this.context;
			}
			else if (ccl != null) {
				currentContextPerThread.put(ccl, this.context);
			}

			if (logger.isInfoEnabled()) {
				long elapsedTime = System.currentTimeMillis() - startTime;
				logger.info("Root WebApplicationContext initialized in " + elapsedTime + " ms");
			}

			return this.context;
		}
		catch (RuntimeException | Error ex) {
			logger.error("Context initialization failed", ex);
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, ex);
			throw ex;
		}
	}
 ```
&emsp;&emsp;initWebApplicationContext 函数主要就是体现了创建 WebApplicationContext 实例的一个功能架构，从函数中我们看到了初始化的大致步骤。
1. WebApplicationContxt 存在性的验证。
&emsp;&emsp;配置中只允许声明一次 ServletContextListener，多次声明会扰乱 Spring 的执行逻辑，所以这里首先要做的就是对比验证，在 Spring 中如果创建 WebApplicationContext 实例会记录在 ServletContext 中以方便全局调用，而使用的 key 就是 WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE ,所以验证的方式就是查看 ServletContext 实例中是否又对应 key 的属性。
2. 创建 WebApplicationContext 实例。
&emsp;&emsp;如果通过验证，则 Spring 将创建 WebApplicationContext 实例的工作委托给了 createWebApplicationContext 函数。
 ```java
    static {
		// Load default strategy implementations from properties file.
		// This is currently strictly internal and not meant to be customized
		// by application developers.
		try {
			ClassPathResource resource = new ClassPathResource(DEFAULT_STRATEGIES_PATH, ContextLoader.class);
			defaultStrategies = PropertiesLoaderUtils.loadProperties(resource);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Could not load 'ContextLoader.properties': " + ex.getMessage());
		}
	}
    
    protected WebApplicationContext createWebApplicationContext(ServletContext sc) {
		Class<?> contextClass = determineContextClass(sc);
		if (!ConfigurableWebApplicationContext.class.isAssignableFrom(contextClass)) {
			throw new ApplicationContextException("Custom context class [" + contextClass.getName() +
					"] is not of type [" + ConfigurableWebApplicationContext.class.getName() + "]");
		}
		return (ConfigurableWebApplicationContext) BeanUtils.instantiateClass(contextClass);
	}

    protected Class<?> determineContextClass(ServletContext servletContext) {
		String contextClassName = servletContext.getInitParameter(CONTEXT_CLASS_PARAM);
		if (contextClassName != null) {
			try {
				return ClassUtils.forName(contextClassName, ClassUtils.getDefaultClassLoader());
			}
			catch (ClassNotFoundException ex) {
				throw new ApplicationContextException(
						"Failed to load custom context class [" + contextClassName + "]", ex);
			}
		}
		else {
			contextClassName = defaultStrategies.getProperty(WebApplicationContext.class.getName());
			try {
				return ClassUtils.forName(contextClassName, ContextLoader.class.getClassLoader());
			}
			catch (ClassNotFoundException ex) {
				throw new ApplicationContextException(
						"Failed to load default context class [" + contextClassName + "]", ex);
			}
		}
	}
 ```
&emsp;&emsp;根据以上静态代码块的内容，我们推断在当前类 ContextLoader 同样目录下必定会存在 属性文件 ContextLoader.properties , 查看后果然存在，内容如下：
org.springframework.web.context.WebApplicationContext=org.springframework.web.context.support.XmlWebApplicationContext
&emsp;&emsp;综合以上代码分析，并根据其中的配置提取将要实现 WebApplicationContext 接口的实现类，并根据这个实现类通过反射的方式进行实例的创建。
3. 将实例记录在 servletContext 中。
4. 映射当前的类加载器与创建的实例到全局变量 currentContextPerThread 中。

## 11.3 DispatcherServlet
&emsp;&emsp;在 Spring 中，ContextLoaderListener 只是辅助功能，用于创建 WebApplicationContext 类型实例，而真正的逻辑实现其实是在 DispatcherServlet 中进行，DispatcherServlet 是实现 servlet 接口的实现类。
&emsp;&emsp;servlet 是一个Java编写的程序，此程序是基于HTTP协议的，在服务器端运行的(如Tomcat)，是按照 servlet 规范编写的一个Java类。主要是处理客户端的请求将其结果发送给客户端。servlet的声明周期是由servlet的容器来控制的，它可以分为3个阶段：初始化、运行和销毁。
### 1. 初始化阶段
* servlet 容器加载 servlet 类，把servlet类的.class文件中的数据读到内存中。
* servlet 容器创建一个 ServletConfig 对象。 ServletConfig 对象包含了 servlet 的初始化配置信息。
* servlet 容器创建一个 servlet 对象。
* servlet 容器调用 servlet 对象的 init 方法进行初始化。
### 2. 运行阶段
&emsp;&emsp;当 servlet 容器接收到一个请求时， servlet 容器会针对这个请求创建 servletRequest 和 servletResponse 对象，然后调用 service 方法。并将这两个参数传递给 service 方法。service 方法通过 servletReqeust 对象获得请求的信息。并处理该请求。再通过servletResponse对象生成这个请求的响应结果。然后销毁 servletRequest 和 servletResponse 对象。我们不管这个请求是post提交的还是get提交的，最终这个请求都会由 service 方法来处理。
### 3. 销毁阶段
&emsp;&emsp;当 Web 应用被终止时，servlet 容器会先调用 servlet 对象的 destroy 方法，然后在销毁 servlet 对象，同时也会销毁与servlet对象相关联的 servletConfig 对象。我们可以在destroy方法的实现中，释放servlet所占用的资源，如关闭数据库连接，关闭文件输入输出流等。
&emsp;&emsp;servlet 的框架是由两个Java包组成： javax.servlet 和 javax.servlet.http 。 在 javax.servlet 包中定义了所有的 servlet 类都必须实现或者扩展的通用接口和类， 在 javax.servlet.http 包中定义了 采用 HTTP 通信协议和 HttpServlet 类。
&emsp;&emsp; servlet 被设计成请求驱动， servlet 的请求可能包含多个数据项，当 Web 容器🉑️到某个 servlet 请求时，servlet 把请求封装成一个 HttpServletReqeust 对象，然后把对象传给 servlet 的对应的服务方法。
&emsp;&emsp;HTTP 的请求方式包括 delete、get、potions、post、put和trace，在 HttpServlet 类中分别提供了相应的服务方法，它们是 doDelete()、doGet()、doOptions()、doPost()、doPut()和doTrace()。

### 11.3.1 servlet 的使用
&emsp;&emsp;servlet的使用我们这里就不赘述了。网上可以随意找找都由相关介绍。
### 11.3.2 DispatcherServlet 的初始化
&emsp;&emsp;通过上面的实例我们了解到，在 servlet 初始化阶段会调用其 init 方法，所以我们首先要查看在 DispatcherServlet 中是否重写了 init 方法。 我们在其父类HttpServletBean中早到该方法。
 ```java
    /**
	 * Map config parameters onto bean properties of this servlet, and
	 * invoke subclass initialization.
	 * @throws ServletException if bean properties are invalid (or required
	 * properties are missing), or if subclass initialization fails.
	 */
	@Override
	public final void init() throws ServletException {

		// Set bean properties from init parameters.
		// 解析 init-param 并封装至 pvs 中
		PropertyValues pvs = new ServletConfigPropertyValues(getServletConfig(), this.requiredProperties);
		if (!pvs.isEmpty()) {
			try {
				//将当前的这个servlet 转化为一个 BeanWrapper，从而能够以Spring的方式来对 init-param 值进行注入
				BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(this);
				ResourceLoader resourceLoader = new ServletContextResourceLoader(getServletContext());
				//注册自定义属性编辑器，一旦遇到 Resource 类型的属性将会使用 ResourceEditor进行解析
				bw.registerCustomEditor(Resource.class, new ResourceEditor(resourceLoader, getEnvironment()));
				//空实现，留给之类覆盖
				initBeanWrapper(bw);
				//属性注入
				bw.setPropertyValues(pvs, true);
			}
			catch (BeansException ex) {
				if (logger.isErrorEnabled()) {
					logger.error("Failed to set bean properties on servlet '" + getServletName() + "'", ex);
				}
				throw ex;
			}
		}

		// Let subclasses do whatever initialization they like.
		//留给之类扩展
		initServletBean();
	}
 ```
&emsp;&emsp;DispatcherServlet 的初始化过程主要是通过将当前的 servlet 类型实例转换为 BeanWrapper 类型实例，以便使用 Spring 中提供的注入功能进行对应属性的注入。这些属性如 contextAttribute、contextClass、nameSpace、contextConfigLocation 等，都可以在 web.xml 文件中以初始化参数的方式配置在 servlet 的声明中。DispatcherServlet 继承自 FrameworkServlet，FrameworkServlet 类上包含对应的同名属性，Spring 会保证这些参数被注入到对应的值中。属性注入主要包含一下几个步骤。
1. 封装及验证初始化参数
ServletConfigPropertyValues 除了封装属性外还有对属性验证的功能。
 ```java
    /**
        * Create new ServletConfigPropertyValues.
        * @param config the ServletConfig we'll use to take PropertyValues from
        * @param requiredProperties set of property names we need, where
        * we can't accept default values
        * @throws ServletException if any required properties are missing
        */
    public ServletConfigPropertyValues(ServletConfig config, Set<String> requiredProperties)
            throws ServletException {

        Set<String> missingProps = (!CollectionUtils.isEmpty(requiredProperties) ?
                new HashSet<>(requiredProperties) : null);

        Enumeration<String> paramNames = config.getInitParameterNames();
        while (paramNames.hasMoreElements()) {
            String property = paramNames.nextElement();
            Object value = config.getInitParameter(property);
            addPropertyValue(new PropertyValue(property, value));
            if (missingProps != null) {
                missingProps.remove(property);
            }
        }

        // Fail if we are still missing properties.
        if (!CollectionUtils.isEmpty(missingProps)) {
            throw new ServletException(
                    "Initialization from ServletConfig for servlet '" + config.getServletName() +
                    "' failed; the following required properties were missing: " +
                    StringUtils.collectionToDelimitedString(missingProps, ", "));
        }
    }
 ```
&emsp;&emsp;从代码中得知，封装属性主要是对初始化的参数进行封装，也就是 servlet 中配置的 <init-param> 中配置的封装。当然，用户可以通过对 requiredProperties 参数的初始化来强制验证某些属性的必要性，这样，在属性封装的过程中，一旦检测到 requiredProperties 中的属性没有指定初始化值，就会抛出异常。
2. 将当前 servlet 实例转换成 BeanWrapper 实例
&emsp;&emsp;PropertyAccessorFactory.forBeanPropertyAccess 是 Spring 中提供的工具方法，主要用于将指定实例转换为 Spring 中可以处理的 BeanWrapper 类型的实例。
3. 注册相对于 Resource 的属性编辑器
&emsp;&emsp;属性编辑器，我们在上文中已经介绍并且分析过其原理，这里使用属性编辑器的目的是在对当前实例(DispatcherServlet)属性注入过程中一旦遇到 Resource 类型的属性就会使用 ResourceEditor 去解析。
4. 属性注入
&emsp;&emsp;BeanWrapper为Spring中的方法，支持Spring的自动注入。其实我们最常用的属性注入无非是 contextAttribute、contextClass、nameSpace、contextConfigLocation 等。
5. servletBean 的初始化
&emsp;&emsp;在 ContextLoaderListener 加载的时候已经创建了 WebApplicationContext 实例，而在这个函数中最重要的就是对这个实例进行进一步的补充初始化。
&emsp;&emsp;继续查看 initServletBean()。 父类 FrameworkServlet 覆盖了 HttpServletBean 中的 initServletBean 函数，如下：
 ```java
    /**
	 * Overridden method of {@link HttpServletBean}, invoked after any bean properties
	 * have been set. Creates this servlet's WebApplicationContext.
	 */
	@Override
	protected final void initServletBean() throws ServletException {
		getServletContext().log("Initializing Spring " + getClass().getSimpleName() + " '" + getServletName() + "'");
		if (logger.isInfoEnabled()) {
			logger.info("Initializing Servlet '" + getServletName() + "'");
		}
		long startTime = System.currentTimeMillis();

		try {
			//初始化webApplicationContext
			this.webApplicationContext = initWebApplicationContext();
			//设计为之类覆盖
			initFrameworkServlet();
		}
		catch (ServletException | RuntimeException ex) {
			logger.error("Context initialization failed", ex);
			throw ex;
		}

		if (logger.isDebugEnabled()) {
			String value = this.enableLoggingRequestDetails ?
					"shown which may lead to unsafe logging of potentially sensitive data" :
					"masked to prevent unsafe logging of potentially sensitive data";
			logger.debug("enableLoggingRequestDetails='" + this.enableLoggingRequestDetails +
					"': request parameters and headers will be " + value);
		}

		if (logger.isInfoEnabled()) {
			logger.info("Completed initialization in " + (System.currentTimeMillis() - startTime) + " ms");
		}
	}
 ```
&emsp;&emsp;上面的函数设计了计时来统计初始化的执行时间，而且提供了一个扩展方法initFrameworkServlet()用于之类的覆盖操作，而作为关键的初始化逻辑实现委托给了 initWebApplicationContext()。
### 11.3.3 WebApplicationContext 的初始化
&emsp;&emsp;initWebApplicationContext 函数的主要工作就是创建或刷新 WebApplicationContext 实例并对 servlet 功能说使用的变量进行初始化。
 ```java
    /**
	 * Initialize and publish the WebApplicationContext for this servlet.
	 * <p>Delegates to {@link #createWebApplicationContext} for actual creation
	 * of the context. Can be overridden in subclasses.
	 * @return the WebApplicationContext instance
	 * @see #FrameworkServlet(WebApplicationContext)
	 * @see #setContextClass
	 * @see #setContextConfigLocation
	 */
	protected WebApplicationContext initWebApplicationContext() {
		WebApplicationContext rootContext =
				WebApplicationContextUtils.getWebApplicationContext(getServletContext());
		WebApplicationContext wac = null;

		if (this.webApplicationContext != null) {
			// A context instance was injected at construction time -> use it
			//context 实例在构造函数中被注入
			wac = this.webApplicationContext;
			if (wac instanceof ConfigurableWebApplicationContext) {
				ConfigurableWebApplicationContext cwac = (ConfigurableWebApplicationContext) wac;
				if (!cwac.isActive()) {
					// The context has not yet been refreshed -> provide services such as
					// setting the parent context, setting the application context id, etc
					if (cwac.getParent() == null) {
						// The context instance was injected without an explicit parent -> set
						// the root application context (if any; may be null) as the parent
						cwac.setParent(rootContext);
					}
					//刷新上下文环境
					configureAndRefreshWebApplicationContext(cwac);
				}
			}
		}
		if (wac == null) {
			// No context instance was injected at construction time -> see if one
			// has been registered in the servlet context. If one exists, it is assumed
			// that the parent context (if any) has already been set and that the
			// user has performed any initialization such as setting the context id
			//根据 contextAttribute 属性加载 WebApplicationContext
			wac = findWebApplicationContext();
		}
		if (wac == null) {
			// No context instance is defined for this servlet -> create a local one
			wac = createWebApplicationContext(rootContext);
		}

		if (!this.refreshEventReceived) {
			// Either the context is not a ConfigurableApplicationContext with refresh
			// support or the context injected at construction time had already been
			// refreshed -> trigger initial onRefresh manually here.
			synchronized (this.onRefreshMonitor) {
				onRefresh(wac);
			}
		}

		if (this.publishContext) {
			// Publish the context as a servlet context attribute.
			String attrName = getServletContextAttributeName();
			getServletContext().setAttribute(attrName, wac);
		}

		return wac;
	}
 ```
&emsp;&emsp;对于本函数中的初始化主要包含几个部分。
#### 1. 寻找或创建对应的 WebApplicationContext 实例
WebApplicationContext 的寻找以及创建包括一下几个步骤。
1. 通过构造函数的注入进行初始化。
2. 通过 contextAttribute 进行初始化。
通过在 web.xml 文件中配置的 servlet 参数 contextAttribute 来查 ServletContext 中对应的属性，默认为 WebApplicationContext.class.getName() + ".ROOT" ，也就是在 ContextLoaderListener 加载时会创建 WebApplicationContext 实例，并将实例以 WebApplicationContext.class.getName() + ".ROOT" 为key 放入ServletContext 中，当然我们也可以重写 ContextListener逻辑使用自己创建的 WebApplicationContext，并在 servlet 配置中通过初始化参数 contextAttribute 指定 key。
 ```java
    @Nullable
	protected WebApplicationContext findWebApplicationContext() {
		String attrName = getContextAttribute();
		if (attrName == null) {
			return null;
		}
		WebApplicationContext wac =
				WebApplicationContextUtils.getWebApplicationContext(getServletContext(), attrName);
		if (wac == null) {
			throw new IllegalStateException("No WebApplicationContext found: initializer not registered?");
		}
		return wac;
	}
 ```
3. 重新创建 WebApplicationContext 实例。
&emsp;&emsp;如果以上两种方式都没有找到任何突破，那就没办法了，只能在这里重新创建新的实例了。
 ```java
    protected WebApplicationContext createWebApplicationContext(@Nullable WebApplicationContext parent) {
		return createWebApplicationContext((ApplicationContext) parent);
    }
    protected WebApplicationContext createWebApplicationContext(@Nullable ApplicationContext parent) {
		//获取 servlet 初始化参数 contextClass，如果没有配置默认为 XmlWebApplicationContext.class
		Class<?> contextClass = getContextClass();
		if (!ConfigurableWebApplicationContext.class.isAssignableFrom(contextClass)) {
			throw new ApplicationContextException(
					"Fatal initialization error in servlet with name '" + getServletName() +
					"': custom WebApplicationContext class [" + contextClass.getName() +
					"] is not of type ConfigurableWebApplicationContext");
		}
		//通过反射方式实例化 contextClass
		ConfigurableWebApplicationContext wac =
				(ConfigurableWebApplicationContext) BeanUtils.instantiateClass(contextClass);
		//设置环境比变量
		wac.setEnvironment(getEnvironment());
		//parent 为在 ContextLoaderListener 中创建的实例
		wac.setParent(parent);
		//获取 contextConfigLocation 属性，配置在 servlet 初始化参数中
		String configLocation = getContextConfigLocation();
		if (configLocation != null) {
			wac.setConfigLocation(configLocation);
		}
		//初始化Spring环境包括加载配置文件等
		configureAndRefreshWebApplicationContext(wac);

		return wac;
	}
 ```
### 2. configureAndRefreshWebApplicationContext
&emsp;&emsp;不论是通过构造函数注入还是单独创建，都会调用 configureAndRefreshWebApplicationContext 方法来对已经创建的 WebApplicationContext 实例进行配置及刷新，那么这个步骤又做了哪些工作呢？
 ```java
 protected void configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext wac) {
		if (ObjectUtils.identityToString(wac).equals(wac.getId())) {
			// The application context id is still set to its original default value
			// -> assign a more useful id based on available information
			if (this.contextId != null) {
				wac.setId(this.contextId);
			}
			else {
				// Generate default id...
				wac.setId(ConfigurableWebApplicationContext.APPLICATION_CONTEXT_ID_PREFIX +
						ObjectUtils.getDisplayString(getServletContext().getContextPath()) + '/' + getServletName());
			}
		}

		wac.setServletContext(getServletContext());
		wac.setServletConfig(getServletConfig());
		wac.setNamespace(getNamespace());
		wac.addApplicationListener(new SourceFilteringListener(wac, new ContextRefreshListener()));

		// The wac environment's #initPropertySources will be called in any case when the context
		// is refreshed; do it eagerly here to ensure servlet property sources are in place for
		// use in any post-processing or initialization that occurs below prior to #refresh
		ConfigurableEnvironment env = wac.getEnvironment();
		if (env instanceof ConfigurableWebEnvironment) {
			((ConfigurableWebEnvironment) env).initPropertySources(getServletContext(), getServletConfig());
		}

		postProcessWebApplicationContext(wac);
		applyInitializers(wac);
		//加载配置文件及整合parent到wac
		wac.refresh();
	}
 ```
&emsp;&emsp;无论调用方式如何变化，只要是使用 ApplicationContext 说提供的功能最后都免不了使用公共父类 AbstractApplicationContext 提供的 refresh() 进行配置文件加载。

### 3. 刷新
&emsp;&emsp;onRefresh 是 FreameworkServlet 类中提供的模板方法，在其之类 DispatcherServlet 中进行了重写，主要用于刷新 Spring 在 Web 功能实现中所必须使用的全局变量。下面我们会介绍它们的初始化过程已经使用场景，而至于具体的功能细节会在稍后的张杰中再做详细介绍。
 ```java
 	@Override
	protected void onRefresh(ApplicationContext context) {
		initStrategies(context);
	}

	/**
	 * Initialize the strategy objects that this servlet uses.
	 * <p>May be overridden in subclasses in order to initialize further strategy objects.
	 */
	protected void initStrategies(ApplicationContext context) {
		//初始化 MultipartResolver
		initMultipartResolver(context);
		//初始化 LocaleResolver
		initLocaleResolver(context);
		//初始化 initThemeResolver
		initThemeResolver(context);
		//初始化 HandlerMappings
		initHandlerMappings(context);
		//初始化 HandlerAdapters
		initHandlerAdapters(context);
		//初始化 HandlerExceptionResolvers
		initHandlerExceptionResolvers(context);
		//初始化 RequestToViewNameTranslator
		initRequestToViewNameTranslator(context);
		//初始化 ViewResolvers
		initViewResolvers(context);
		//初始化 FlashMapManager
		initFlashMapManager(context);
	}
 ```
#### 1. 初始化 MultipartResolver。
&emsp;&emsp;在 Spring 中,MultipartResolver 主要用来处理文件上传。默认情况下，Spring 是没有 multipart 处理的，因为一些开发者想要自己处理它们。如果想使用 Spring 的 multipart，则需要在 Web 应用的上文中添加 multipart 解析器。这样，每个请求就会被检查是否包含multipart。然而，如果请求中包含 multipart ，那么上下文中定义的 MultipartResolver 就会解析它，这样请求中的 multipart 属性就会像其他属性一样被处理。通常配置如下：
 ```xml
 	<bean id="multipartResolver" class="org.springframework.web.multipart.commons.CommonsMultipartResolver">
        <property name="maxUploadSize">
            <value>10000</value>
        </property>
    </bean>
 ```
&emsp;&emsp;当然，CommonsmultipartResolver 还提供了其他功能用于帮助用户完成上传功能，有兴趣的读者可以进一步查看。
&emsp;&emsp;MultipartResolver 就是在 initMultipartResolver 中被加入到 DispatherServlet 中的。
 ```java
 private void initMultipartResolver(ApplicationContext context) {
		try {
			this.multipartResolver = context.getBean(MULTIPART_RESOLVER_BEAN_NAME, MultipartResolver.class);
			if (logger.isTraceEnabled()) {
				logger.trace("Detected " + this.multipartResolver);
			}
			else if (logger.isDebugEnabled()) {
				logger.debug("Detected " + this.multipartResolver.getClass().getSimpleName());
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Default is no multipart resolver.
			this.multipartResolver = null;
			if (logger.isTraceEnabled()) {
				logger.trace("No MultipartResolver '" + MULTIPART_RESOLVER_BEAN_NAME + "' declared");
			}
		}
	}
 ```
&emsp;&emsp;因为之前的步骤已经完成了 Spring 中配置文件的解析，所以在这里只要在配置文件注册过都可以通过 ApplicationContext 提供的 getBean 方法来直接获取对应 bean， 今儿初始化 MultipartResolver 中的 multipartResolver 变量。
#### 2. 初始化 LocaleResolver。
&emsp;&emsp;在 Spring 的国际化配置中一共有3种使用方式。
* 基于 URL 参数的配置。
&emsp;&emsp;通过URL参数来控制国际化，比如你在页面上加依据<a href="?locale=zh_CN">简体中文</a>来控制项目中使用的国际化参数。而提供这个功能的就是 AcceptHeaderLocaleResolver，默认的参数名为locale，注意大小写。里面放的就是你的提交参数，比如 en_US、zh_CN之类的，具体配置如下：
 ```xml
 <bean id="localeResolver" class="org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver"/>
 ```
* 基于 session 的配置。
&emsp;&emsp;它通过检验用户会话中预置的属性来解析区域。最常用的是根据用户本次会话过程中的语言设定决定语言种类(例如，用户登录时选择语言种类，则此次登录周期内统一使用此语言设定)，如果该会话属性不存在，它会根据 accept-language HTTP 投不确认默认区域。
 ```xml
 <bean id="localeResolver" class="org.springframework.web.servlet.i18n.SessionLocaleResolver" />
 ```
* 基于 cookie 的国际化配置。
&emsp;&emsp;CookieLocalResolver 用于通过浏览器的 cookie 设置取到 Locale 对象。 这种策略在应用程序不支持会话或者状态保存在客户端时有用，配置如下：
 ```xml
 <bean id="localeResolver" class="org.springframework.web.servlet.i18n.CookieLocaleResolver" />
 ```
&emsp;&emsp;这3种方式都可以解决国际化问题，但是，对于 LocalResolver 的使用基础是在 DispatherServlet 中的初始化。
 ```java
 	private void initLocaleResolver(ApplicationContext context) {
		try {
			this.localeResolver = context.getBean(LOCALE_RESOLVER_BEAN_NAME, LocaleResolver.class);
			if (logger.isTraceEnabled()) {
				logger.trace("Detected " + this.localeResolver);
			}
			else if (logger.isDebugEnabled()) {
				logger.debug("Detected " + this.localeResolver.getClass().getSimpleName());
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// We need to use the default.
			this.localeResolver = getDefaultStrategy(context, LocaleResolver.class);
			if (logger.isTraceEnabled()) {
				logger.trace("No LocaleResolver '" + LOCALE_RESOLVER_BEAN_NAME +
						"': using default [" + this.localeResolver.getClass().getSimpleName() + "]");
			}
		}
	}
 ```
&emsp;&emsp;提取配置文件中的 LocaleResolver 来初始化 DispathcerServlet 中的 localeResolver 属性。

#### 3. 初始化 ThemeResolver。
&emsp;&emsp;在 Web 开发中经常会遇到通过主题 Theme 来控制网页风格，这将进一步改善用户体验。简单地说，一个主题就是一组静态资源(比如样式表和图片)，它们可以影响应用程序的视觉效果。Spring 中的主题功能和国际化功能非常类似。Spring 主题功能的结构主要包括如下内容。
* **主题资源**
&emsp;&emsp;org.springframework.ui.context.ThemeSource 是 Spring 中主题资源的接口，Spring 的主题需要通过 ThemeSource 接口来实现存放主题信息的资源。
&emsp;&emsp;org.springframework.ui.context.support.ResourceBundleThemeSource 是 ThemeSource 接口默认实现类 (也就是通过 ResourceBundler 资源的方式定义主题),在 Spring 中的配置如下：
 ```xml
	<bean id="themeSource" class="org.springframework.ui.context.support.ResourceBundleThemeSource">
        <property name="basenamePrefix"value="info.test."></property>
	</bean>
 ```
&emsp;&emsp;默认状态下是在类目录下查找相应的资源文件，也可以通过 basenamePrefix 来定制。这样， DispathcerServlet 就会在 info.test 包下查找资源文件。
* **主题解析器**
&emsp;&emsp;ThemeSource 定义了一些主题资源，那么不同的用户使用什么主题资源由谁定义呢？org.springframework.web.servlet.ThemeResolver 是主题解析器的接口，主题解析器的工作便由它的子类来完成。
&emsp;&emsp;对于主题解析器的子类主要有3个比较常用的实现。以主题文件 summer.properties 为例。
1. FixedThemeResolver 用于选择一个固定的主题。
 ```xml
  	<bean id="themeResolver" class="org.springframework.web.servlet.theme.FixedThemeResolver">
        <property name="defaultThemeName" value="summer"/>
    </bean>
 ```
&emsp;&emsp;以上配置的作用是设置主题文件为 summer.properties ，在整个项目内固定不变。
2. CookieThemeResolver 用于实现用户所选的主题，以 cookie 的形式存放在客户端的机器上，配置如下：
 ```xml
 	<bean id="themeResolver" class="org.springframework.web.servlet.theme.CookieThemeResolver">
        <property name="defaultThemeName" value="summer"/>
    </bean>
 ```
3. SessionThemeResolver 用于主题保存在用户的 HTTP Session 中。
 ```xml
 	<bean id="themeResolver" class="org.springframework.web.servlet.theme.SessionThemeResolver">
        <property name="defaultThemeName" value="summer"/>
    </bean>
 ```
4. AbstractThemeResolver 是一个抽象类被 SessionThemeResolver 和 FixedThemeResolver 继承，用户也可以继承它来自定义主题解析器。
* **拦截器**
如果需要根据用户请求来改变主题，那么 Spring 提供了一个已经实现的拦截器——ThemeChangeInterceptor 拦截器了，配置如下：
 ```xml
 	<bean id="themeChangeInterceptor" class="org.springframework.web.servlet.theme.ThemeChangeInterceptor">
        <property name="paramName" value="themeName"></property>
    </bean>
 ```
&emsp;&emsp;其中设置用户请求参数名为 themeName， 即URL为?themeName=具体的主题名称。此外，还需要在 handlerMapping 中配置拦截器。当然需要在 HandlerMapping 中添加拦截器。
 ```xml
 <property name="interceptors">
 	<list>
	 	<ref local="themeChangeInterceptor">
	 </list>
 </property>
 ```
&emsp;&emsp;了解了主题文件的简单实用方式后，再来查看解析器的初始化工作，与其他变量的初始化工作相同，主题文件解析器的初始化工作并没有需要特别说明的地方。
 ```java
 	private void initThemeResolver(ApplicationContext context) {
		try {
			this.themeResolver = context.getBean(THEME_RESOLVER_BEAN_NAME, ThemeResolver.class);
			if (logger.isTraceEnabled()) {
				logger.trace("Detected " + this.themeResolver);
			}
			else if (logger.isDebugEnabled()) {
				logger.debug("Detected " + this.themeResolver.getClass().getSimpleName());
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// We need to use the default.
			this.themeResolver = getDefaultStrategy(context, ThemeResolver.class);
			if (logger.isTraceEnabled()) {
				logger.trace("No ThemeResolver '" + THEME_RESOLVER_BEAN_NAME +
						"': using default [" + this.themeResolver.getClass().getSimpleName() + "]");
			}
		}
	}
 ```
#### 4. 初始化 HandlerMappings
&emsp;&emsp;当客户端发出 Request 时 DispatcherServlet 会将 request 提交给 HandlerMapping， 然后 HandlerMapping 根据 WebApplicationContext 的配置来回传给 DispatcherServlet 相应的 Controller。
&emsp;&emsp;在基于 SpringMVC 的 Web 应用程序中，我们可以为 DispatcherServlet 提供多个 HandlerMapping 供其使用。 DispatcherServlet 在选用 HandlerMapping 的过程中，将根据我们说指定的一系列 HandlerMapping 的优先级进行排序，然后优先使用优先级在前的 HandlerMapping 。 如果当前的 HandlerMapping 能够返回可用的 Handler， DispatcherServlet 则使用当前返回的 Handler 进行Web请求的处理，而不再继续询问其他的 HandlerMapping 。 否则，DispatcherServlet 将继续按照各个 HandlerMapping 优先级进行询问，直到获取一个可用的 Handler 为止。 初始化配置如下：
 ```java
	private void initHandlerMappings(ApplicationContext context) {
		this.handlerMappings = null;

		if (this.detectAllHandlerMappings) {
			// Find all HandlerMappings in the ApplicationContext, including ancestor contexts.
			Map<String, HandlerMapping> matchingBeans =
					BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerMapping.class, true, false);
			if (!matchingBeans.isEmpty()) {
				this.handlerMappings = new ArrayList<>(matchingBeans.values());
				// We keep HandlerMappings in sorted order.
				AnnotationAwareOrderComparator.sort(this.handlerMappings);
			}
		}
		else {
			try {
				HandlerMapping hm = context.getBean(HANDLER_MAPPING_BEAN_NAME, HandlerMapping.class);
				this.handlerMappings = Collections.singletonList(hm);
			}
			catch (NoSuchBeanDefinitionException ex) {
				// Ignore, we'll add a default HandlerMapping later.
			}
		}

		// Ensure we have at least one HandlerMapping, by registering
		// a default HandlerMapping if no other mappings are found.
		if (this.handlerMappings == null) {
			this.handlerMappings = getDefaultStrategies(context, HandlerMapping.class);
			if (logger.isTraceEnabled()) {
				logger.trace("No HandlerMappings declared for servlet '" + getServletName() +
						"': using default strategies from DispatcherServlet.properties");
			}
		}
	}
 ```
&emsp;&emsp;默认情况下，SpringMVC 将加载当前系统中所有实现了 HandlerMapping 接口的bean。如果只期望 SpringMVC 加载指定的 handlermapping 时，可以修改 web.xml 中的 DispatcherServlet 的初始参数， 将 detectAllHandlerMappings 的值设置为 false：
 ```xml
 <init-param>
	<param-name>detectAllHandlerMapping</param-name>
	<param-value>false</param-value>
 </init-param>
 ```
&emsp;&emsp;此时，SpringMVC 将查找名为 "handlerMapping" 的 bean，并作为当前系统中唯一的handlermapping。 如果没有定义 handlerMapping 的话，则 SpringMVC 将按照 org.springframework.web.servlet.DispatcherServlet 所在目录下 DispatcherServlet.properties 中所定义 org.springframework.web.servlet.HandlerMapping 的内容来加载默认的 handlerMapping (用户没有自定义 Strategies的情况下)。

#### 5. 初始化 HandlerAdapters
&emsp;&emsp;从名字也能联想到这是一个典型的适配器模式的使用，在计算机编程中，适配器模式将一个类的接口适配成用户所其他的。使用适配器，可以使接口不兼容而无法在一起工作的类协同工作，做法是将类自己的接口包裹在一个已存在的类中。那么在处理 handler 时为什么会使用适配器模式呢？回答这个问题我们首先要分析它的初始化逻辑。
 ```java
 	private void initHandlerAdapters(ApplicationContext context) {
		this.handlerAdapters = null;

		if (this.detectAllHandlerAdapters) {
			// Find all HandlerAdapters in the ApplicationContext, including ancestor contexts.
			Map<String, HandlerAdapter> matchingBeans =
					BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerAdapter.class, true, false);
			if (!matchingBeans.isEmpty()) {
				this.handlerAdapters = new ArrayList<>(matchingBeans.values());
				// We keep HandlerAdapters in sorted order.
				AnnotationAwareOrderComparator.sort(this.handlerAdapters);
			}
		}
		else {
			try {
				HandlerAdapter ha = context.getBean(HANDLER_ADAPTER_BEAN_NAME, HandlerAdapter.class);
				this.handlerAdapters = Collections.singletonList(ha);
			}
			catch (NoSuchBeanDefinitionException ex) {
				// Ignore, we'll add a default HandlerAdapter later.
			}
		}

		// Ensure we have at least some HandlerAdapters, by registering
		// default HandlerAdapters if no other adapters are found.
		if (this.handlerAdapters == null) {
			this.handlerAdapters = getDefaultStrategies(context, HandlerAdapter.class);
			if (logger.isTraceEnabled()) {
				logger.trace("No HandlerAdapters declared for servlet '" + getServletName() +
						"': using default strategies from DispatcherServlet.properties");
			}
		}
	}
 ```
&emsp;&emsp;同样在初始化的过程中涉及了一个变量 detectAllHandlerAdapters，detectAllHandlerAdapters作用和 detectAllHandlerMapping 类似，只不过作用对象为 handlerAdapter。亦可通过如下配置来强制系统只加载 bean name 为 “handlerAdapter” handlerAdapter。
 ```xml
	<init-param>
		<param-name>detectAllHandlerAdapters</param-name>
		<param-value>false</param-value>
	</init-param>
 ```
&emsp;&emsp;如果无法找到对应的bean，那么系统会尝试加载默认的适配器。
 ```java
 	protected <T> List<T> getDefaultStrategies(ApplicationContext context, Class<T> strategyInterface) {
		String key = strategyInterface.getName();
		String value = defaultStrategies.getProperty(key);
		if (value != null) {
			String[] classNames = StringUtils.commaDelimitedListToStringArray(value);
			List<T> strategies = new ArrayList<>(classNames.length);
			for (String className : classNames) {
				try {
					Class<?> clazz = ClassUtils.forName(className, DispatcherServlet.class.getClassLoader());
					Object strategy = createDefaultStrategy(context, clazz);
					strategies.add((T) strategy);
				}
				catch (ClassNotFoundException ex) {
					throw new BeanInitializationException(
							"Could not find DispatcherServlet's default strategy class [" + className +
							"] for interface [" + key + "]", ex);
				}
				catch (LinkageError err) {
					throw new BeanInitializationException(
							"Unresolvable class definition for DispatcherServlet's default strategy class [" +
							className + "] for interface [" + key + "]", err);
				}
			}
			return strategies;
		}
		else {
			return new LinkedList<>();
		}
	}
 ```
&emsp;&emsp;在 getDefaultStrategies 函数中，Spring 会尝试从 defaultStrategies 中加载对应的 HandlerAdapter 的属性，那么 defaultStrategies 是如何初始化的呢？
&emsp;&emsp;在当前 DispatcherServlet 中存在这样一段初始化代码：
 ```java
 private static final String DEFAULT_STRATEGIES_PATH = "DispatcherServlet.properties";
 static {
		// Load default strategy implementations from properties file.
		// This is currently strictly internal and not meant to be customized
		// by application developers.
		try {
			ClassPathResource resource = new ClassPathResource(DEFAULT_STRATEGIES_PATH, DispatcherServlet.class);
			defaultStrategies = PropertiesLoaderUtils.loadProperties(resource);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Could not load '" + DEFAULT_STRATEGIES_PATH + "': " + ex.getMessage());
		}
	}
 ```
&emsp;&emsp;系统加载的时候，defaultStrategies 根据当前路径 DispatcherServlet.properties 来初始化本身，查看 DispatcherServlet.properties 中对应的 HandlerAdapter 的属性：
 ```properties
 org.springframework.web.servlet.HandlerAdapter=org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter,\
	org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter,\
	org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter,\
	org.springframework.web.servlet.function.support.HandlerFunctionAdapter
 ```
&emsp;&emsp;由此得知，如果开发人员没有在配置文件中定义自己的适配器，那么Spring会默认加载配置文件中的3个适配器。
&emsp;&emsp;作为总控制器的派遣器 servlet 通过处理器映射得到处理器后，会轮训处理器适配器模块，查询能够处理当前HTTP请求的处理器适配器的实现，处理器适配器模块根据处理器映射返回处理器类型，例如简单的控制器类型、注解控制器类型或者远程调用处理器类型，来选择某个适当的处理器适配器的实现，从而适配当前HTTP请求。
* HTTP请求处理器适配器(HttpRequestHandlerAdapter)。
&emsp;&emsp;HTTP 请求处理器适配器仅仅支持对HTTP请求处理器的适配。它简单地将HTTP请求对象和响应对象传输给HTTP请求处理器的实现，它并不需要返回值。它主要应用在基于HTTP的远程调用的实现上。
* 简单控制器处理适配器(SimpleControllerHandlerAdapter)。
&emsp;&emsp;这个实现类将HTTP请求适配到一个控制器的实现进行处理。这里控制器的实现是一个简单的控制器接口的实现。简单控制器处理器适配器被设计成一个框架类的实现，不需要被改写，客户化的业务逻辑通常是控制器接口的实现类中实现的。

#### 6. 初始化 HanlderExceptionResolvers
&emsp;&emsp;基于 HandlerExceptionResolver 接口的异常处理，使用这种方式只需要实现 resolveException 方法，该方法返回一个 ModelAndView 对象，在方法内部对异常的类型进行判断，然后尝试生成对应的 ModelAndView 对象，如果该方法返回了 null，则 Spring 会继续寻找其他的实现了 HandlerExceptionResolver 接口的bean。换句话说，Spring会搜索所有注册在其环境中的实现了HandlerExceptionResolver接口的bean，逐个执行，直到返回一个ModelAndView对象。
&emsp;&emsp; HandlerExceptionResolver 的初始化逻辑和 上面HandlerAdapters、HandlerMapping 的大体逻辑都一样。详细代码如下：
 ```java
 	private void initHandlerExceptionResolvers(ApplicationContext context) {
		this.handlerExceptionResolvers = null;

		if (this.detectAllHandlerExceptionResolvers) {
			// Find all HandlerExceptionResolvers in the ApplicationContext, including ancestor contexts.
			Map<String, HandlerExceptionResolver> matchingBeans = BeanFactoryUtils
					.beansOfTypeIncludingAncestors(context, HandlerExceptionResolver.class, true, false);
			if (!matchingBeans.isEmpty()) {
				this.handlerExceptionResolvers = new ArrayList<>(matchingBeans.values());
				// We keep HandlerExceptionResolvers in sorted order.
				AnnotationAwareOrderComparator.sort(this.handlerExceptionResolvers);
			}
		}
		else {
			try {
				HandlerExceptionResolver her =
						context.getBean(HANDLER_EXCEPTION_RESOLVER_BEAN_NAME, HandlerExceptionResolver.class);
				this.handlerExceptionResolvers = Collections.singletonList(her);
			}
			catch (NoSuchBeanDefinitionException ex) {
				// Ignore, no HandlerExceptionResolver is fine too.
			}
		}

		// Ensure we have at least some HandlerExceptionResolvers, by registering
		// default HandlerExceptionResolvers if no other resolvers are found.
		if (this.handlerExceptionResolvers == null) {
			this.handlerExceptionResolvers = getDefaultStrategies(context, HandlerExceptionResolver.class);
			if (logger.isTraceEnabled()) {
				logger.trace("No HandlerExceptionResolvers declared in servlet '" + getServletName() +
						"': using default strategies from DispatcherServlet.properties");
			}
		}
	}
 ```
#### 7. 初始化 RequestToViewNameTranslator
&emsp;&emsp;当 Controller 处理器方法没有返回一个 View 对象或逻辑视图名称，并且在该方法中没有直接往 response 的输出流里面写数据的时候，Spring 就会采用约定好的方式提供一个逻辑视图名称。这个逻辑视图名称是通过 Spring 定义的 org.springframework.web.servlet.RequestToViewNameTranslator 接口的 getViewName 方法来实现的，我们可以实现自己的 RequestToViewNameTranslator 接口来约定没有返回视图名称的时候如果确定视图名称。Spring 已经给我们提供了一个它自己的实现，那就是 org.springframework.web.servlet.view.DefaultRequestToViewNameTranslator。
&emsp;&emsp;在介绍 DefaultRequestToViewNameTranslator 是如何约定视图名称之前，先来看一下它支持用户定义的属性。
* prefix:前缀，表示约定好的视图名称需要加上前缀，默认是空串。
* suffix:后缀，表示约定好的视图名称需要加上后缀，默认是空串。
* separator:分隔符，默认是斜杠"/"。
* stripLeadingSlash:如果首字符是分隔符，是否需要去除，默认是true。
* stripTrailingSlash:如果最后一个字符是分隔符，是否需要去除，默认是true。
* urlDecode:是否需要对URL解码，默认是true。它会采用 request 指定的编码或者ISO-8859-1编码对URL进行解码。
&emsp;&emsp;当我们没有在 SpringMVC 的配置文件中手动定义一个名为 viewNameTranslator 的bean时，Spring就会默认提供一个即 DefaultRequestToViewNameTranslator 。
&emsp;&emsp;接下来看一下，当Controller处理器方法没有返回逻辑视图名称时，DefaultRequestToViewNameTranslator 是如何约定视图名称的。DefaultRequestToViewNameTranslator 会获取到请求的URI，然后根据提供的属性进行一些改造，把改造之后的结果作为视图名返回。这里以请求路径 http://localhost/app/test/index.html 为例，来说明一下 DefaultRequestToViewNameTranslator 是如何工作的。该请求路径对应的请求 URI 为 /test/index.html ，我们来看一下几种情况，它分别对应的逻辑视图名称是什么。
* prefix 和 suffix 如果都存在，其他为默认值，那么对应返回的逻辑视图名称应该是 prefixtext/indexsuffix。
* stripLeadingSlash和stripExtension都为false，其他默认，这时候视图名称应该是/test/index.html。
* 如果采用默认配置时，返回的逻辑视图名称应该是 test/index。
&emsp;&emsp;如果逻辑视图名称跟请求路径相同或者相关关系都是一样的，那么我们就可以采用 Spring 为我们事先约定好的逻辑视图名称返回，这可以大大简化我们的开发工作，而以上功能实现关键属性 viewNameTranslator，则是在 initRequestToViewNameTranslator 中完成的。
 ```java
 	private void initRequestToViewNameTranslator(ApplicationContext context) {
		try {
			this.viewNameTranslator =
					context.getBean(REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME, RequestToViewNameTranslator.class);
			if (logger.isTraceEnabled()) {
				logger.trace("Detected " + this.viewNameTranslator.getClass().getSimpleName());
			}
			else if (logger.isDebugEnabled()) {
				logger.debug("Detected " + this.viewNameTranslator);
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// We need to use the default.
			this.viewNameTranslator = getDefaultStrategy(context, RequestToViewNameTranslator.class);
			if (logger.isTraceEnabled()) {
				logger.trace("No RequestToViewNameTranslator '" + REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME +
						"': using default [" + this.viewNameTranslator.getClass().getSimpleName() + "]");
			}
		}
	}
 ```
#### 8. 初始化 ViewResolvers
&emsp;&emsp;在 SpringMVC 中，当Controller将请求处理结果行到 ModelAndView 中以后，DispatcherServlet 会根据 ModelAndView 选择合适的视图进行渲染。那么在 SpringMVC 中时如何选择适合的 View 呢？ View 对象是如何创建呢？ 答案就在 ViewResolver 中。 ViewResolver 接口定义了 resolverViewName 方法，根据viewName创建合适类型的View实现。
&emsp;&emsp;那么如何配置 ViewResolver 呢？在Spring中，ViewResolver作为 SpringBean存在，可以在Spring配置文件中进行配置，例如下面的代码，配置JSP相关的viewResolver。
 ```xml
 	<bean id="viewResolver" class="org.springframework.web.servlet.view.InternalResourceViewResolver">
        <property name="prefix" value="/WEB-INF/jsp/"/>
        <property name="suffix" value=".jsp"/>
    </bean>
 ```
&emsp;&emsp;viewResolvers 属性的初始化工作值 initViewResolvers 中完成。初始化的逻辑就不贴代码了，与上面几个的大体逻辑一样。
#### 9. 初始化 FlashMapManager
&emsp;&emsp;SpringMVC Flash attributes 提供了一个请求存储属性，可以供其他请求使用。在使用重定向的时候非常有必要，例如 Post/Redirect/Get模式。Flash attributes 在重定向之前缓存(就像 session中)以便重定向之后还能使用，并立即删除。
&emsp;&emsp;SpringMVC有两个主要的抽象来支持 flash attributes。FlashMap 用于保持 flash attributes，而 FlashMapManager 用于存储、检索、管理FlashMap实例。
&emsp;&emsp;flash attribute 支持默认开启("on")并不需要显示启用，它永远不会导致 HTTP Session 的创建。这两个FlashMap实例都可以通过静态方法 RequestContextUtils 从SpringMVC的任何位置访问。其初始化在 initFlashMapManager 中完成。代码也不贴了，就是一个简单的从context中获取bean。

## 11.4 DispatcherServlet 的逻辑处理
&emsp;&emsp;根据之前的了解，我们知道 HttpServlet 类中分别提供了相应服务方法，它们是 doDelete()、 doGet()、 doOptions()、 doPost()、 doPut()和doTrace()，它会根究请求的不同形式将程序引导至对应的函数进行处理。这几个函数中最常用的函数无非就是 doGet() 和 doPost()，那么我们就直接查看 DispatcherServlet 中对于两个函数的逻辑实现。
 ```java
 	@Override
	protected final void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		processRequest(request, response);
	}

	@Override
	protected final void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		processRequest(request, response);
	}
 ```
&emsp;&emsp;对于不同的方法,Spring并没有做特殊处理，而是统一将程序再一次引导至 processRequest(request,response)中。
 ```java
 	protected final void processRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		//记录当前时间，用于计算web请求的处理时间
		long startTime = System.currentTimeMillis();
		Throwable failureCause = null;

		LocaleContext previousLocaleContext = LocaleContextHolder.getLocaleContext();
		LocaleContext localeContext = buildLocaleContext(request);

		RequestAttributes previousAttributes = RequestContextHolder.getRequestAttributes();
		ServletRequestAttributes requestAttributes = buildRequestAttributes(request, response, previousAttributes);

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
		asyncManager.registerCallableInterceptor(FrameworkServlet.class.getName(), new RequestBindingInterceptor());

		initContextHolders(request, localeContext, requestAttributes);

		try {
			doService(request, response);
		}
		catch (ServletException | IOException ex) {
			failureCause = ex;
			throw ex;
		}
		catch (Throwable ex) {
			failureCause = ex;
			throw new NestedServletException("Request processing failed", ex);
		}

		finally {
			resetContextHolders(request, previousLocaleContext, previousAttributes);
			if (requestAttributes != null) {
				requestAttributes.requestCompleted();
			}
			logResult(request, response, failureCause, asyncManager);
			publishRequestHandledEvent(request, response, startTime, failureCause);
		}
	}
 ```
&emsp;&emsp;函数中已经开始了对请求的处理，虽然把细节转移到了 doService 函数中实现，但是我们不难看出处理请求前后所做的准备与处理工作。
1. 为了保证当前线程的 LocaleContext 以及 RequestAttributes 可以在当前请求后还能恢复，提取当前线程的两个属性。
2. 根据当前request创建对应的LocaleContext和RequestAttributes，并绑定到当前线程。
3. 委托给doService方法进一步处理。
4. 请求处理结束后恢复线程到原始状态。
5. 请求处理结束后无论成功与否发布事件通知。
继续doService方法。
 ```java
 	@Override
	protected void doService(HttpServletRequest request, HttpServletResponse response) throws Exception {
		//打印 request 各种日志
		logRequest(request);

		// Keep a snapshot of the request attributes in case of an include,
		// to be able to restore the original attributes after the include.
		Map<String, Object> attributesSnapshot = null;
		if (WebUtils.isIncludeRequest(request)) {
			attributesSnapshot = new HashMap<>();
			Enumeration<?> attrNames = request.getAttributeNames();
			while (attrNames.hasMoreElements()) {
				String attrName = (String) attrNames.nextElement();
				if (this.cleanupAfterInclude || attrName.startsWith(DEFAULT_STRATEGIES_PREFIX)) {
					attributesSnapshot.put(attrName, request.getAttribute(attrName));
				}
			}
		}

		// Make framework objects available to handlers and view objects.
		// 使框架对象的handler和view 对象可用
		request.setAttribute(WEB_APPLICATION_CONTEXT_ATTRIBUTE, getWebApplicationContext());
		request.setAttribute(LOCALE_RESOLVER_ATTRIBUTE, this.localeResolver);
		request.setAttribute(THEME_RESOLVER_ATTRIBUTE, this.themeResolver);
		request.setAttribute(THEME_SOURCE_ATTRIBUTE, getThemeSource());

		if (this.flashMapManager != null) {
			FlashMap inputFlashMap = this.flashMapManager.retrieveAndUpdate(request, response);
			if (inputFlashMap != null) {
				request.setAttribute(INPUT_FLASH_MAP_ATTRIBUTE, Collections.unmodifiableMap(inputFlashMap));
			}
			request.setAttribute(OUTPUT_FLASH_MAP_ATTRIBUTE, new FlashMap());
			request.setAttribute(FLASH_MAP_MANAGER_ATTRIBUTE, this.flashMapManager);
		}

		try {
			doDispatch(request, response);
		}
		finally {
			if (!WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted()) {
				// Restore the original attribute snapshot, in case of an include.
				if (attributesSnapshot != null) {
					restoreAttributesAfterInclude(request, attributesSnapshot);
				}
			}
		}
	}
 ```
&emsp;&emsp;我们猜想对请求处理至少应该包括一些诸如寻找 Handler 并页面转跳子类的逻辑处理，但是，在 doService 中我们并没有看到想看到的逻辑，但是，在 doService 中我们并灭有看到想看到的逻辑，相反却同样是一些准备工作，但是这些准备工作却是必不可少的。Spring 将已经初始化的功能辅助功能变量，比如 localeResolver、themeResolver 等设置在 request 属性中，而这些属性会在接下来的处理中派上用场。
&emsp;&emsp;接下来，我们就看看 doDispatch 函数中完整的请求处理过程。
 ```java
 	protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
		HttpServletRequest processedRequest = request;
		HandlerExecutionChain mappedHandler = null;
		boolean multipartRequestParsed = false;

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);

		try {
			ModelAndView mv = null;
			Exception dispatchException = null;

			try {
				/**
				 * 检查 request 是否是 MultipartContent 类型，如果是则转换 request 为 MultipartHttpServletRequest 类型的 request
				 * 使用的就是
				 * @see org.springframework.web.servlet.DispatcherServlet#initMultipartResolver
				 * 判断逻辑就是看 Context-Type 是否是以 "multipart/"开头-使用的是apache的commons-fileupload
				 */
				processedRequest = checkMultipart(request);
				//判断是否是文件request
				multipartRequestParsed = (processedRequest != request);

				// Determine handler for the current request.
				// 决定使用哪个 mappingHandler 去处理 ， 如果没找到返回404
				mappedHandler = getHandler(processedRequest);
				if (mappedHandler == null) {
					noHandlerFound(processedRequest, response);
					return;
				}

				// Determine handler adapter for the current request.
				// 决定使用哪个 adapter handler 适配器
				HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());

				// Process last-modified header, if supported by the handler.
				// 如果 HandlerAdapter 支持，last-modified 头处理
				String method = request.getMethod();
				boolean isGet = "GET".equals(method);
				if (isGet || "HEAD".equals(method)) {
					long lastModified = ha.getLastModified(request, mappedHandler.getHandler());
					if (new ServletWebRequest(request, response).checkNotModified(lastModified) && isGet) {
						return;
					}
				}

				//所有拦截器的 preHandler 方法
				if (!mappedHandler.applyPreHandle(processedRequest, response)) {
					return;
				}

				// Actually invoke the handler.
				// 调用 handler
				mv = ha.handle(processedRequest, response, mappedHandler.getHandler());

				if (asyncManager.isConcurrentHandlingStarted()) {
					return;
				}
				//视图名称转换应用于需要添加前后缀的情况
				applyDefaultViewName(processedRequest, mv);
				//所有拦截器的 postHandler 方法
				mappedHandler.applyPostHandle(processedRequest, response, mv);
			}
			catch (Exception ex) {
				dispatchException = ex;
			}
			catch (Throwable err) {
				// As of 4.3, we're processing Errors thrown from handler methods as well,
				// making them available for @ExceptionHandler methods and other scenarios.
				dispatchException = new NestedServletException("Handler dispatch failed", err);
			}
			// 处理返回结果，是否返回 呈现的视图
			processDispatchResult(processedRequest, response, mappedHandler, mv, dispatchException);
		}
		catch (Exception ex) {
			triggerAfterCompletion(processedRequest, response, mappedHandler, ex);
		}
		catch (Throwable err) {
			triggerAfterCompletion(processedRequest, response, mappedHandler,
					new NestedServletException("Handler processing failed", err));
		}
		finally {
			if (asyncManager.isConcurrentHandlingStarted()) {
				// Instead of postHandle and afterCompletion
				if (mappedHandler != null) {
					mappedHandler.applyAfterConcurrentHandlingStarted(processedRequest, response);
				}
			}
			else {
				// Clean up any resources used by a multipart request.
				if (multipartRequestParsed) {
					cleanupMultipart(processedRequest);
				}
			}
		}
	}

	private void processDispatchResult(HttpServletRequest request, HttpServletResponse response,
			@Nullable HandlerExecutionChain mappedHandler, @Nullable ModelAndView mv,
			@Nullable Exception exception) throws Exception {

		boolean errorView = false;
		// 先判断是否存在异常
		if (exception != null) {
			if (exception instanceof ModelAndViewDefiningException) {
				logger.debug("ModelAndViewDefiningException encountered", exception);
				mv = ((ModelAndViewDefiningException) exception).getModelAndView();
			}
			else {
				Object handler = (mappedHandler != null ? mappedHandler.getHandler() : null);
				mv = processHandlerException(request, response, handler, exception);
				errorView = (mv != null);
			}
		}

		// Did the handler return a view to render?
		// 如果在 Handler 实例的处理中返回了 view，那么需要做页面的处理
		if (mv != null && !mv.wasCleared()) {
			// 处理页面转跳
			render(mv, request, response);
			if (errorView) {
				WebUtils.clearErrorRequestAttributes(request);
			}
		}
		else {
			if (logger.isTraceEnabled()) {
				logger.trace("No view rendering, null ModelAndView returned.");
			}
		}

		if (WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted()) {
			// Concurrent handling started during a forward
			return;
		}

		if (mappedHandler != null) {
			// Exception (if any) is already handled..
			mappedHandler.triggerAfterCompletion(request, response, null);
		}
	}
 ```
&emsp;&emsp;doDispatch 函数中展示了 Spring 请求处理说涉及的主要逻辑，而我们之前设置在 request 中的各种辅助属性也都派上了用场。

### 11.4.1 MultipartContent 类型的request处理
&emsp;&emsp;对于请求的处理，Spring首先考虑的是对于 Multipart 的处理，如果是 MultipartContent 类型的 request，则转换 request 为 MultipartHttpServletRequest 类型的 request。
 ```java
 	protected HttpServletRequest checkMultipart(HttpServletRequest request) throws MultipartException {
		if (this.multipartResolver != null && this.multipartResolver.isMultipart(request)) {
			if (WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class) != null) {
				if (request.getDispatcherType().equals(DispatcherType.REQUEST)) {
					logger.trace("Request already resolved to MultipartHttpServletRequest, e.g. by MultipartFilter");
				}
			}
			else if (hasMultipartException(request)) {
				logger.debug("Multipart resolution previously failed for current request - " +
						"skipping re-resolution for undisturbed error rendering");
			}
			else {
				try {
					return this.multipartResolver.resolveMultipart(request);
				}
				catch (MultipartException ex) {
					if (request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE) != null) {
						logger.debug("Multipart resolution failed for error dispatch", ex);
						// Keep processing error dispatch with regular request handle below
					}
					else {
						throw ex;
					}
				}
			}
		}
		// If not returned before: return original request.
		return request;
	}
 ```
### 11.4.2 根据 request 信息寻找对应的 Handler
&emsp;&emsp;在 Spring 中最简单的映射处理器配置如下 :
 ```xml
 	<bean id="simpleUrlMapping" class="org.springframework.web.servlet.handler.SimpleUrlHandlerMapping">
        <property name="mappings">
            <props>
                <prop key="/userlist.html">userController</prop>
            </props>
        </property>
	</bean>
 ```
&emsp;&emsp;在Spring加载过程中，Spring会将类型为 SimpleUrlHandlerMapping 的实例加载到 this.handlerMapping 中，按照常理推断，根据request提取对应的Handler，无非就是提取当前实例中的userController，但是userContorller为继承自AbstractController类型实例，与HandlerExecutionChain并无任何关联，那么这一步是如何封装的呢？
 ```java
 	@Nullable
	protected HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
		if (this.handlerMappings != null) {
			for (HandlerMapping mapping : this.handlerMappings) {
				HandlerExecutionChain handler = mapping.getHandler(request);
				if (handler != null) {
					return handler;
				}
			}
		}
		return null;
	}
 ```
&emsp;&emsp;之前我们提过，Spring会将所有的映射类型的bean注册到this.handlerMapping变量中。现在我们以SimpleUrlHandlerMapping为例查看其getHanlder方法如下：
 ```java
	//AbstractHandlerMapping.java
 	@Override
	@Nullable
	public final HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
		//根据request获取对应的handler
		Object handler = getHandlerInternal(request);
		//如果没有对应的handler则使用默认的 handler
		if (handler == null) {
			handler = getDefaultHandler();
		}
		//如果也没有提供默认的handler则无法继续处理返回null
		if (handler == null) {
			return null;
		}
		// Bean name or resolved handler?
		if (handler instanceof String) {
			String handlerName = (String) handler;
			handler = obtainApplicationContext().getBean(handlerName);
		}

		HandlerExecutionChain executionChain = getHandlerExecutionChain(handler, request);

		if (logger.isTraceEnabled()) {
			logger.trace("Mapped to " + handler);
		}
		else if (logger.isDebugEnabled() && !request.getDispatcherType().equals(DispatcherType.ASYNC)) {
			logger.debug("Mapped to " + executionChain.getHandler());
		}

		if (hasCorsConfigurationSource(handler)) {
			CorsConfiguration config = (this.corsConfigurationSource != null ? this.corsConfigurationSource.getCorsConfiguration(request) : null);
			CorsConfiguration handlerConfig = getCorsConfiguration(handler, request);
			config = (config != null ? config.combine(handlerConfig) : handlerConfig);
			executionChain = getCorsHandlerExecutionChain(request, executionChain, config);
		}

		return executionChain;
	}
 ```
&emsp;&emsp;函数首先会使用 getHandlerInternal 方法根据 request 信息获取对应的 Hanlder，如果以 SimpleUrlHandlerMapping 为例分析，那么我们推断此步骤提供的功能很可能就是根据URL找到匹配的Controller并返回，当然如果没有找到对应的Controller 处理器那么程序会尝试去查找配置中的默认处理器，当然，当查找的 controller 为String类型时，那么久意味着返回的是配置的bean名称，需要根据bean名称查找对应的bean，最后，还要通过 getHandlerExecutionChain 方法对返回的 Hanlder 进行封装，以保证满足返回类型的匹配。下面详细分析这个过程。
#### 1. 根据request查找对应的Handler
&emsp;&emsp;首先根据request查找对应的Hanlder开始分析。
 ```java
	/**
	 * Look up a handler for the URL path of the given request.
	 * @param request current HTTP request
	 * @return the handler instance, or {@code null} if none found
	 */
	@Override
	@Nullable
	protected Object getHandlerInternal(HttpServletRequest request) throws Exception {
		//截取用于匹配url有效路径
		String lookupPath = getUrlPathHelper().getLookupPathForRequest(request);
		request.setAttribute(LOOKUP_PATH, lookupPath);
		//根据路径寻找Handler
		Object handler = lookupHandler(lookupPath, request);
		if (handler == null) {
			// We need to care for the default handler directly, since we need to
			// expose the PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE for it as well.
			Object rawHandler = null;
			// 如果请求路径仅仅是 "/"，那么使用 RootHandler 进行处理
			if ("/".equals(lookupPath)) {
				rawHandler = getRootHandler();
			}
			if (rawHandler == null) {
				//无法找到 handler 则使用默认 handler
				rawHandler = getDefaultHandler();
			}
			if (rawHandler != null) {
				// Bean name or resolved handler?
				// 根据 beanName 获取对应的 bean
				if (rawHandler instanceof String) {
					String handlerName = (String) rawHandler;
					rawHandler = obtainApplicationContext().getBean(handlerName);
				}
				//模板方法
				validateHandler(rawHandler, request);
				handler = buildPathExposingHandler(rawHandler, lookupPath, lookupPath, null);
			}
		}
		return handler;
	}

	protected Object lookupHandler(String urlPath, HttpServletRequest request) throws Exception {
		// Direct match?
		Object handler = this.handlerMap.get(urlPath);
		if (handler != null) {
			// Bean name or resolved handler?
			// 根据beanName 获取对应的 bean
			if (handler instanceof String) {
				String handlerName = (String) handler;
				handler = obtainApplicationContext().getBean(handlerName);
			}
			//模板方法
			validateHandler(handler, request);
			return buildPathExposingHandler(handler, urlPath, urlPath, null);
		}

		// Pattern match?
		// 通配符匹配处理
		List<String> matchingPatterns = new ArrayList<>();
		for (String registeredPattern : this.handlerMap.keySet()) {
			if (getPathMatcher().match(registeredPattern, urlPath)) {
				matchingPatterns.add(registeredPattern);
			}
			else if (useTrailingSlashMatch()) {
				if (!registeredPattern.endsWith("/") && getPathMatcher().match(registeredPattern + "/", urlPath)) {
					matchingPatterns.add(registeredPattern + "/");
				}
			}
		}

		String bestMatch = null;
		Comparator<String> patternComparator = getPathMatcher().getPatternComparator(urlPath);
		if (!matchingPatterns.isEmpty()) {
			matchingPatterns.sort(patternComparator);
			if (logger.isTraceEnabled() && matchingPatterns.size() > 1) {
				logger.trace("Matching patterns " + matchingPatterns);
			}
			bestMatch = matchingPatterns.get(0);
		}
		if (bestMatch != null) {
			handler = this.handlerMap.get(bestMatch);
			if (handler == null) {
				if (bestMatch.endsWith("/")) {
					handler = this.handlerMap.get(bestMatch.substring(0, bestMatch.length() - 1));
				}
				if (handler == null) {
					throw new IllegalStateException(
							"Could not find handler for best pattern match [" + bestMatch + "]");
				}
			}
			// Bean name or resolved handler?
			if (handler instanceof String) {
				String handlerName = (String) handler;
				handler = obtainApplicationContext().getBean(handlerName);
			}
			validateHandler(handler, request);
			String pathWithinMapping = getPathMatcher().extractPathWithinPattern(bestMatch, urlPath);

			// There might be multiple 'best patterns', let's make sure we have the correct URI template variables
			// for all of them
			Map<String, String> uriTemplateVariables = new LinkedHashMap<>();
			for (String matchingPattern : matchingPatterns) {
				if (patternComparator.compare(bestMatch, matchingPattern) == 0) {
					Map<String, String> vars = getPathMatcher().extractUriTemplateVariables(matchingPattern, urlPath);
					Map<String, String> decodedVars = getUrlPathHelper().decodePathVariables(request, vars);
					uriTemplateVariables.putAll(decodedVars);
				}
			}
			if (logger.isTraceEnabled() && uriTemplateVariables.size() > 0) {
				logger.trace("URI variables " + uriTemplateVariables);
			}
			return buildPathExposingHandler(handler, bestMatch, pathWithinMapping, uriTemplateVariables);
		}

		// No handler found...
		return null;
	}
 ```
&emsp;&emsp;根据URL获取对应的Handler的匹配规则代码实现起来虽然很长，但是并不难理解，考虑了直接匹配和通配符两种情况。其中要提及的是 buildPathExposingHandler 函数，它将 Handler 封装成了 HandlerExecutionChain 类型。
 ```java
 	protected Object buildPathExposingHandler(Object rawHandler, String bestMatchingPattern,
			String pathWithinMapping, @Nullable Map<String, String> uriTemplateVariables) {

		HandlerExecutionChain chain = new HandlerExecutionChain(rawHandler);
		chain.addInterceptor(new PathExposingHandlerInterceptor(bestMatchingPattern, pathWithinMapping));
		if (!CollectionUtils.isEmpty(uriTemplateVariables)) {
			chain.addInterceptor(new UriTemplateVariablesHandlerInterceptor(uriTemplateVariables));
		}
		return chain;
	}
 ```
&emsp;&emsp;在函数中我们看到了通过将 Handler 以参数形式传入，并构建 HandlerExecutionChain 类型实例，加入了 两个拦截器。此时我们似乎已经了解了 Spring 这样大费周章的目的。链处理机制，是Spring中非常常用的处理方式，是AOP中的重要组成部分，可以方便地对目标对象进行扩展及拦截，这是非常优秀的设计。

#### 2. 加入拦截器到执行链
&emsp;&emsp; getHandlerExecutionChain 函数的主要目的是将配置中的对应拦截器加入到执行链中，以保证这些拦截器可以有效地作用于目标对象。
 ```java
 	protected HandlerExecutionChain getHandlerExecutionChain(Object handler, HttpServletRequest request) {
		HandlerExecutionChain chain = (handler instanceof HandlerExecutionChain ?
				(HandlerExecutionChain) handler : new HandlerExecutionChain(handler));

		String lookupPath = this.urlPathHelper.getLookupPathForRequest(request, LOOKUP_PATH);
		for (HandlerInterceptor interceptor : this.adaptedInterceptors) {
			if (interceptor instanceof MappedInterceptor) {
				MappedInterceptor mappedInterceptor = (MappedInterceptor) interceptor;
				if (mappedInterceptor.matches(lookupPath, this.pathMatcher)) {
					chain.addInterceptor(mappedInterceptor.getInterceptor());
				}
			}
			else {
				chain.addInterceptor(interceptor);
			}
		}
		return chain;
	}
 ```
### 11.4.3 没有找到对应的Handler的错误处理
&emsp;&emsp;每一个请求都应该对应这一 Handler，因为每个请求都会在后台有相应的逻辑对应，而逻辑的实现就是在Handler中，所以一旦遇到没有找到Handler的情况(正常情况如果没有URL匹配的Handler，开发热暖可以设置默认的Handler来处理请求，但是如果默认请求也未设置就会出现Handler为空的情况)，就只能通过response向用户返回错误信息。
 ```java
 	protected void noHandlerFound(HttpServletRequest request, HttpServletResponse response) throws Exception {
		if (pageNotFoundLogger.isWarnEnabled()) {
			pageNotFoundLogger.warn("No mapping for " + request.getMethod() + " " + getRequestUri(request));
		}
		if (this.throwExceptionIfNoHandlerFound) {
			throw new NoHandlerFoundException(request.getMethod(), getRequestUri(request),
					new ServletServerHttpRequest(request).getHeaders());
		}
		else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}
 ```
### 11.4.4 根据当前 Handler 寻找对应的 HandlerAdapter
&emsp;&emsp;在WebApplicationContext的初始化过程中我们讨论了 HandlerAdapters 的初始化，了解了默认情况下普通的 Web 请求会交给 SimpleControllerHandlerAdapter 去处理。下面我们以 SimpleControllerHandlerAdapter 为例来分析获取适配器的逻辑。
 ```java
 	protected HandlerAdapter getHandlerAdapter(Object handler) throws ServletException {
		if (this.handlerAdapters != null) {
			for (HandlerAdapter adapter : this.handlerAdapters) {
				if (adapter.supports(handler)) {
					return adapter;
				}
			}
		}
		throw new ServletException("No adapter for handler [" + handler +
				"]: The DispatcherServlet configuration needs to include a HandlerAdapter that supports this handler");
	}
 ```
&emsp;&emsp;通过上看的函数我们了解到，对于获取适配器的逻辑无非就是遍历所有适配器来选择适合的适配器并返回它，而某个适配器是否适用于当前的 Handler 逻辑被封装在具体的适配器中。进一步查看SimpleControllerHandlerAdapter 中的 supports 方法。
 ```java
 	@Override
	public boolean supports(Object handler) {
		return (handler instanceof Controller);
	}
 ```
&emsp;&emsp;分析到这里，一切已经明了，SimpleControllerhandlerAdapter 就是用于处理普通的 Web 请求，而对于 SpringMVC 来说，我们会把逻辑封装至 Controller 之类中，例如我们之前的引导示例 UserController 就是继承自 AbstractController，而 AbstractController 实现 Controller 接口。
### 11.4.5 缓存处理
&emsp;&emsp;在研究 Spring对缓存处理的功能支持前，我们先了解一个概念: Last-Modified缓存机制 。 
1. 在客户端第一次输入 URL 时， 服务器端会返回内容和状态码 200，表示请求成功， 同时会添加 一个“Last-Modified”的响应头，表示此文件在服务器上的最后更新时间，例如，“Last-Modified:Wed,14 Mar 2012 10:22:42 GMT”表示最后更新时间为( 2012-03-14 10:22 )。
2. 客户端第二次请求此 URL 时，客户端会向服务器发送请求头“If-Modified-Since”，询问服务器该时间之后当前请求内容是否有被修改过，如“If-Modified-Since: Wed, 14 Mar 2012 10:22:42 GMT”，如果服务器端的内容没有变化， 则自动返回 HTTP 304 状态码(只要响应头， 内容为空 ，这样就节省了网络带宽)。
&emsp;&emsp;Spring 提供的对 Last-Modified 机制的支持，只需要实现 LastModified 接口，如下所示：
 ```java
 public class HelloWorldLastModifiedCacheController extends AbstractController implements LastModified {
    private long lastModified;
    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
        //点击后再次请求当前页面
        httpServletResponse.getWriter().write("<a href=''>this</a>");
        return null;
    }

    @Override
    public long getLastModified(HttpServletRequest httpServletRequest) {
        if(lastModified == 0L){
            //第一次或者逻辑有变化的时候，应该重新返回内容最新修改的时间戳
            lastModified = System.currentTimeMillis();
        }
        return lastModified;
    }
}
 ```
&emsp;&emsp;HelloWorldLastModifiedCacheController 只需要实现 LastModified 接口的 getLastModified 方法，保证当内容发生改变时返回最新的修改时间即可。
&emsp;&emsp;Spring 判断是否过期，通过判断请求的 “If-Modified-Since” 是否大于等于当前的 getLastModified 方法的时间戳，如果时，则认为没有修改。上面的 controller 与普通 controller 并无太大差别，声明如下：
 ```xml
 <bean name="helloLastModified" class="info.tonylee.springframework.web.HelloWorldLastModifiedCacheController" />
 ```
### 11.4.6 HandlerInterceptor 的处理
&emsp;&emsp;Servlet API 定义的servlet过滤器可以在 servlet 处理每个 Web 请求的前后分别对它进行前置处理和猴子处理。此外，有些时候，你可能只想处理由某些 SpringMVC 处理程序处理的Web请求，并在这些处理程序返回的模型属性被传递到视图之前，对它们进行一些操作。
&emsp;&emsp;SpringMVC允许你通过处理拦截Web请求，进行前置和后置处理。处理拦截时在 Spring 的 Web 应用程序上线文中配置，因此它们可以利用各种容器特性，并引用容器中声明的任何bean。处理拦截时针对特殊的处理程序映射进行注册，因此它只拦截通过这些处理程序映射的请求。每个处理拦截器都必须实现 HandlerInterceptor 接口，它包含三个需要你实现的回调方法：preHandle()、postHandler()和afterCompletion()。第一个和第二个分别是在处理程序处理请求之前和之后被调用。第二个方法还允许访问返回的ModelAndView对象，因此可以在它里面操作模型属性。最后一个方法是在请求处理完成之后被调用的(如视图呈现之后)，以下是 HandlerInterceptor 的简单实现：
 ```java
 public class MyTestInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        long startTime = System.currentTimeMillis();
        request.setAttribute("startTime", startTime);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        long startTime = (Long)request.getAttribute("startTime");
        long endTime = System.currentTimeMillis();
        modelAndView.addObject("handlingTime", endTime-startTime);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
    }
}
 ```
&emsp;&emsp;这个拦截器的 preHandler() 方法中，你记录了起始时间，并将它记录到请求属性中。这个方法应该返回true，允许DispatherServlet继续处理请求。否则，DispatcherServlet 会认为这个方法已经处理请求，直接将相应返回给用户。然后，在postHandler()方法中，从属性中加载起始时间，并将它与当前时间进行比较。你可以计算总的持续时间，然后把这个时间添加到模型中，传递给视图。最后，afterCompletion方法无事可做，空着就可以了。
### 11.4.7 逻辑处理
&emsp;&emsp;对应逻辑处理起始是通过适配器中转调Handler并返回视图的，对应的代码如下：
 ```java
// 调用 handler
mv = ha.handle(processedRequest, response, mappedHandler.getHandler());
 ```
&emsp;&emsp;同样，还是以引导示例为基础进行处理逻辑分析，之前分析过，对于普通的 Web 请求，Spring默认使用 SimpleControllerHandlerAdapter 类型进行处理，我们进入 SimpleControllerHandlerAdapter 类的handle方法如下：
 ```java
 	@Override
	@Nullable
	public ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		return ((Controller) handler).handleRequest(request, response);
	}
 ```
&emsp;&emsp;但是回顾应用示例中的UserController，我们的逻辑是写在 handleRequestInternal 函数中而不是 handleRequest 函数，所以我们还需要进一步分析这期间说包含的处理流程。
 ```java
	//AbstractController
 	@Override
	@Nullable
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		if (HttpMethod.OPTIONS.matches(request.getMethod())) {
			response.setHeader("Allow", getAllowHeader());
			return null;
		}

		// Delegate to WebContentGenerator for checking and preparing.
		checkRequest(request);
		prepareResponse(response);

		// Execute handleRequestInternal in synchronized block if required.
		// 如果需要session类的同步执行
		if (this.synchronizeOnSession) {
			HttpSession session = request.getSession(false);
			if (session != null) {
				Object mutex = WebUtils.getSessionMutex(session);
				synchronized (mutex) {
					// 调用用户逻辑
					return handleRequestInternal(request, response);
				}
			}
		}
		//调用用户逻辑
		return handleRequestInternal(request, response);
	}
 ```
### 11.4.8 异常视图的处理
&emsp;&emsp;有时候系统运行过程中出现异常，而我们并不希望就此中断对用户的服务，而是至少告知客户当前系统在处理逻辑的过程中出现了异常，甚至告知他们因为什么原因导致的。Spring中的异常处理机制会帮我们完成这个工作。其实，这里Spring主要的工作就是将逻辑引导至 HandlerExceptionResolver 类的 resolveException 方法，而 HandlerExceptionResolver 的使用，我们在讲解 WebApplicationContext 的初始化的时候已经介绍过了。
 ```java
 	@Nullable
	protected ModelAndView processHandlerException(HttpServletRequest request, HttpServletResponse response,
			@Nullable Object handler, Exception ex) throws Exception {

		// Success and error responses may use different content types
		request.removeAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);

		// Check registered HandlerExceptionResolvers...
		ModelAndView exMv = null;
		if (this.handlerExceptionResolvers != null) {
			for (HandlerExceptionResolver resolver : this.handlerExceptionResolvers) {
				exMv = resolver.resolveException(request, response, handler, ex);
				if (exMv != null) {
					break;
				}
			}
		}
		if (exMv != null) {
			if (exMv.isEmpty()) {
				request.setAttribute(EXCEPTION_ATTRIBUTE, ex);
				return null;
			}
			// We might still need view name translation for a plain error model...
			if (!exMv.hasView()) {
				String defaultViewName = getDefaultViewName(request);
				if (defaultViewName != null) {
					exMv.setViewName(defaultViewName);
				}
			}
			if (logger.isTraceEnabled()) {
				logger.trace("Using resolved error view: " + exMv, ex);
			}
			else if (logger.isDebugEnabled()) {
				logger.debug("Using resolved error view: " + exMv);
			}
			WebUtils.exposeErrorRequestAttributes(request, ex, getServletName());
			return exMv;
		}

		throw ex;
	}
 ```
### 11.4.9 根据视图跳转页面
&emsp;&emsp;无论是一个系统还是一个站点，最终要的工作都是与用户进行交付，用户操作系统后无论下发的命令成功与否都需要给用户一个反馈，以便于用户进行下一步的判断。所以，在逻辑处理的最后一定会涉及一个页面转跳的问题。
 ```java
 	protected void render(ModelAndView mv, HttpServletRequest request, HttpServletResponse response) throws Exception {
		// Determine locale for request and apply it to the response.
		Locale locale =
				(this.localeResolver != null ? this.localeResolver.resolveLocale(request) : request.getLocale());
		response.setLocale(locale);

		View view;
		String viewName = mv.getViewName();
		if (viewName != null) {
			// We need to resolve the view name.
			view = resolveViewName(viewName, mv.getModelInternal(), locale, request);
			if (view == null) {
				throw new ServletException("Could not resolve view with name '" + mv.getViewName() +
						"' in servlet with name '" + getServletName() + "'");
			}
		}
		else {
			// No need to lookup: the ModelAndView object contains the actual View object.
			view = mv.getView();
			if (view == null) {
				throw new ServletException("ModelAndView [" + mv + "] neither contains a view name nor a " +
						"View object in servlet with name '" + getServletName() + "'");
			}
		}

		// Delegate to the View object for rendering.
		if (logger.isTraceEnabled()) {
			logger.trace("Rendering view [" + view + "] ");
		}
		try {
			if (mv.getStatus() != null) {
				response.setStatus(mv.getStatus().value());
			}
			view.render(mv.getModelInternal(), request, response);
		}
		catch (Exception ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Error rendering view [" + view + "]", ex);
			}
			throw ex;
		}
	}
 ```
1. **解析视图名称**
&emsp;&emsp;在上文中我们提到了DispatcherServlet会根据ModelAndView选择适合的视图来进行渲染，而这一功能就是在 resolveViewName 函数中完成的。
 ```java
 	@Nullable
	protected View resolveViewName(String viewName, @Nullable Map<String, Object> model,
			Locale locale, HttpServletRequest request) throws Exception {

		if (this.viewResolvers != null) {
			for (ViewResolver viewResolver : this.viewResolvers) {
				View view = viewResolver.resolveViewName(viewName, locale);
				if (view != null) {
					return view;
				}
			}
		}
		return null;
	}
 ```
&emsp;&emsp;我们以 org.springframework.web.servlet.view.InternalResourceViewResolver 为例来分析 ViewResolver 逻辑的解析过程，其中 resolveViewName 函数的实现是在其父类 AbstractCachingViewResolver 中完成的。
 ```java
 	@Override
	@Nullable
	public View resolveViewName(String viewName, Locale locale) throws Exception {
		if (!isCache()) {
			//不存在缓存的情况直接创建视图
			return createView(viewName, locale);
		}
		else {
			// 直接从缓存中提取
			Object cacheKey = getCacheKey(viewName, locale);
			View view = this.viewAccessCache.get(cacheKey);
			if (view == null) {
				synchronized (this.viewCreationCache) {
					view = this.viewCreationCache.get(cacheKey);
					if (view == null) {
						// Ask the subclass to create the View object.
						view = createView(viewName, locale);
						if (view == null && this.cacheUnresolved) {
							view = UNRESOLVED_VIEW;
						}
						if (view != null && this.cacheFilter.filter(view, viewName, locale)) {
							this.viewAccessCache.put(cacheKey, view);
							this.viewCreationCache.put(cacheKey, view);
						}
					}
				}
			}
			else {
				if (logger.isTraceEnabled()) {
					logger.trace(formatKey(cacheKey) + "served from cache");
				}
			}
			return (view != UNRESOLVED_VIEW ? view : null);
		}
	}
 ```
&emsp;&emsp;在父类 UrlBasedViewResolver 中重写了 createView 函数。
 ```java
 	// UrlBasedViewResolver
 	@Override
	protected View createView(String viewName, Locale locale) throws Exception {
		// If this resolver is not supposed to handle the given view,
		// return null to pass on to the next resolver in the chain.
		// 如果当前解析器不支持给定的视图，则返回null以传递到链中下一个解析器。
		if (!canHandle(viewName, locale)) {
			return null;
		}

		// Check for special "redirect:" prefix.
		// 处理前缀为 redirect:xx 的情况
		if (viewName.startsWith(REDIRECT_URL_PREFIX)) {
			String redirectUrl = viewName.substring(REDIRECT_URL_PREFIX.length());
			RedirectView view = new RedirectView(redirectUrl,
					isRedirectContextRelative(), isRedirectHttp10Compatible());
			String[] hosts = getRedirectHosts();
			if (hosts != null) {
				view.setHosts(hosts);
			}
			return applyLifecycleMethods(REDIRECT_URL_PREFIX, view);
		}

		// Check for special "forward:" prefix.
		// 处理前缀为 forward:xx 的情况
		if (viewName.startsWith(FORWARD_URL_PREFIX)) {
			String forwardUrl = viewName.substring(FORWARD_URL_PREFIX.length());
			InternalResourceView view = new InternalResourceView(forwardUrl);
			return applyLifecycleMethods(FORWARD_URL_PREFIX, view);
		}

		// Else fall back to superclass implementation: calling loadView.
		return super.createView(viewName, locale);
	}

	// AbstractCachingViewResolver
	@Nullable
	protected View createView(String viewName, Locale locale) throws Exception {
		return loadView(viewName, locale);
	}

	// UrlBasedViewResolver
	@Override
	protected View loadView(String viewName, Locale locale) throws Exception {
		AbstractUrlBasedView view = buildView(viewName);
		View result = applyLifecycleMethods(viewName, view);
		return (view.checkResource(locale) ? result : null);
	}

	protected AbstractUrlBasedView buildView(String viewName) throws Exception {
		Class<?> viewClass = getViewClass();
		Assert.state(viewClass != null, "No view class");

		AbstractUrlBasedView view = (AbstractUrlBasedView) BeanUtils.instantiateClass(viewClass);
		// 添加前缀以及后缀
		view.setUrl(getPrefix() + viewName + getSuffix());
		view.setAttributesMap(getAttributesMap());

		String contentType = getContentType();
		if (contentType != null) {
			//设置Context-Type
			view.setContentType(contentType);
		}

		String requestContextAttribute = getRequestContextAttribute();
		if (requestContextAttribute != null) {
			view.setRequestContextAttribute(requestContextAttribute);
		}

		Boolean exposePathVariables = getExposePathVariables();
		if (exposePathVariables != null) {
			view.setExposePathVariables(exposePathVariables);
		}
		Boolean exposeContextBeansAsAttributes = getExposeContextBeansAsAttributes();
		if (exposeContextBeansAsAttributes != null) {
			view.setExposeContextBeansAsAttributes(exposeContextBeansAsAttributes);
		}
		String[] exposedContextBeanNames = getExposedContextBeanNames();
		if (exposedContextBeanNames != null) {
			view.setExposedContextBeanNames(exposedContextBeanNames);
		}

		return view;
	}
 ```
&emsp;&emsp;大致阅读以上代码，我们发现对于 InternalResourceViewResolver 所提供的解析功能主要考虑到了几个方面的处理。
* 基于效率考虑，提供了缓存的支持。
* 提供了对 redirect:xx 和 forward:xx 前缀的支持。
* 添加了前缀及后缀，并向View中加入了必须的属性设置。
2. **页面转跳**
&emsp;&emsp;当通过 viewName 解析到对应的View后，就可以进一步的处理跳转逻辑了。
 ```java
 	@Override
	public void render(@Nullable Map<String, ?> model, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		if (logger.isDebugEnabled()) {
			logger.debug("View " + formatViewName() +
					", model " + (model != null ? model : Collections.emptyMap()) +
					(this.staticAttributes.isEmpty() ? "" : ", static attributes " + this.staticAttributes));
		}

		Map<String, Object> mergedModel = createMergedOutputModel(model, request, response);
		prepareResponse(request, response);
		renderMergedOutputModel(mergedModel, getRequestToExpose(request), response);
	}
 ```
&emsp;&emsp;在引导示例中，我们了解到对于 ModelView 的使用，可以将一些属性直接放入其中，然后再页面上直接通过JSTL语法或者原始的request获取。这是一个很方便也很神奇的功能，但是实现却并不复杂，无非是把我们将要用到的属性放入request中，以便在其他地方可以直接调用，而解析这些属性的工作就是在 createMergedOutputModel 函数中完成的。
 ```java
 	protected Map<String, Object> createMergedOutputModel(@Nullable Map<String, ?> model,
			HttpServletRequest request, HttpServletResponse response) {

		@SuppressWarnings("unchecked")
		Map<String, Object> pathVars = (this.exposePathVariables ?
				(Map<String, Object>) request.getAttribute(View.PATH_VARIABLES) : null);

		// Consolidate static and dynamic model attributes.
		int size = this.staticAttributes.size();
		size += (model != null ? model.size() : 0);
		size += (pathVars != null ? pathVars.size() : 0);

		Map<String, Object> mergedModel = new LinkedHashMap<>(size);
		mergedModel.putAll(this.staticAttributes);
		if (pathVars != null) {
			mergedModel.putAll(pathVars);
		}
		if (model != null) {
			mergedModel.putAll(model);
		}

		// Expose RequestContext?
		if (this.requestContextAttribute != null) {
			mergedModel.put(this.requestContextAttribute, createRequestContext(request, response, mergedModel));
		}

		return mergedModel;
	}

	//InternalResourceView
	@Override
	protected void renderMergedOutputModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		// Expose the model object as request attributes.
		exposeModelAsRequestAttributes(model, request);

		// Expose helpers as request attributes, if any.
		exposeHelpers(request);

		// Determine the path for the request dispatcher.
		String dispatcherPath = prepareForRendering(request, response);

		// Obtain a RequestDispatcher for the target resource (typically a JSP).
		RequestDispatcher rd = getRequestDispatcher(request, dispatcherPath);
		if (rd == null) {
			throw new ServletException("Could not get RequestDispatcher for [" + getUrl() +
					"]: Check that the corresponding file exists within your web application archive!");
		}

		// If already included or response already committed, perform include, else forward.
		if (useInclude(request, response)) {
			response.setContentType(getContentType());
			if (logger.isDebugEnabled()) {
				logger.debug("Including [" + getUrl() + "]");
			}
			rd.include(request, response);
		}

		else {
			// Note: The forwarded resource is supposed to determine the content type itself.
			if (logger.isDebugEnabled()) {
				logger.debug("Forwarding to [" + getUrl() + "]");
			}
			rd.forward(request, response);
		}
	}
 ```
