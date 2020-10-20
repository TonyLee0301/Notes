# 7 AOP
&emsp;&emsp;我们知道，面向对象变成(OOP)有一些弊端，当需要为多个不同有继承关系的对象引入同一个公共行为时，例如日志、安全监测等，我们只能在每个对象里应用公共行为，这样程序中就产生了大量的重复代码，程序就不便于维护了，所以就有了一个对面向对象编程的补充，即面向切面编程(AOP)，AOP说关注的方向是横向的，不同于OOP的纵向。
&emsp;&emsp;Spring 中提供了 AOP 的实现，但是在低版本 Spring 中定义一个切面是比较麻烦的，需要实现特定的接口，并进行写较为复杂的配置。低版本 Spring AOP 的配置是被批评最多的地方。 Spring 听取了这方面的批评声音，并下定决心彻底改变这一现状。在 Spring 2.0 中，Spring AOP 已经焕然一新，你可使用 @AspectJ 注解对 POJO 进行标注，从而定义一个包含切点信息和增强横切逻辑的切面。Spring 2.0 可以将这个切面织入到匹配的目标bean中。@AspectJ 注解使用 AspectJ 切面表达式语法进行切面定义，可以通过切点函数、运算符、通配符等高级功能进行切面定义，拥有强大的连续点描述能力。我们先来直观的浏览一下Spring中 AOP 实现。
## 7.1 动态 AOP 使用示例
1. 创建用于创建拦截的 bean
&emsp;&emsp;在实际工作中，此 bean 可能可能是满足业务需要的核心逻辑，例如test方法中可能会封装这某个核心业务，但是，如果我们想在test前后加日志来追踪调试，如果直接修改源码并不符合面向对象的设计方法，而且随意改动原有代码也会造成一定的风险，还好接下来的Spring帮我们做到了这一点。
 ```java
    public class TestBean {
        private String testStr = "testStr";

        public String getTestStr() {
            return testStr;
        }

        public void setTestStr(String testStr) {
            this.testStr = testStr;
        }

        public void test(){
            System.out.println("test");
        }
    }
 ```
2. 创建 Advisor
&emsp;&emsp;Spring 中摒弃了最原始的繁杂配置方式而采用 @AspectJ 注解对POJO进行标注，使AOP的工作大大简化，例如，在 AspectJTest 类中，我们要做的就是在所有类的test方法执行前在控制台打印 beforeTest，而在所有类的test方法执行后打印 afterTest，同时又使用环绕的方式在所有内的方法执行前后再次分别打印 before1 和 after1
 ```java
 @Aspect
    public class AspectJTest {

        @Pointcut("execution(* *.test(..))")
        public void test(){
        }

        @Before("test()")
        public void beforeTest(){
            System.out.println("beforeTest");
        }

        @After("test()")
        public void afterTest(){
            System.out.println("afterTest");
        }


        @Around("test()")
        public Object aroundTest(ProceedingJoinPoint p){
            System.out.println("before1");
            Object o = null;
            try{
                o = p.proceed();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            System.out.println("after1");
            return o;
        }
    }
 ```
3. 创建配置文件
&emsp;&emsp;XML 是 Spring 的基础。尽管 Spring 一再简化配置，但无论如何，至少现在 XML 还是 Spring 的基础(Spring Boot 出来后，这句话可能就需要考量下了)。要在 Spring 中开启 AOP 功能，还需要在配置文件中做如下声明：
 ```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:aop="http://www.springframework.org/schema/aop"
       xmlns:context="http://www.springframework.org/schema/context"

       xsi:schemaLocation="http://www.springframework.org/schema/beans
                        http://www.springframework.org/schema/beans/spring-beans.xsd
                        http://www.springframework.org/schema/aop
                        http://www.springframework.org/schema/aop/spring-aop-4.3.xsd
                        http://www.springframework.org/schema/context
                        http://www.springframework.org/schema/context/spring-context-3.0.xsd">

    <aop:aspectj-autoproxy/>
    <bean id="test" class="info.tonylee.studio.spring.aop.TestBean"/>
    <bean class="info.tonylee.studio.spring.aop.AspectJTest"/>
</beans>
 ```
4. 测试
&emsp;&emsp;接下来我们就可以验证 Spring 的 AOP 为我们提供的神奇效果了。
 ```java
    public static void main(String[] args) {
        ApplicationContext bf = new ClassPathXmlApplicationContext("/META-INF/aop/test-aop.xml");
        TestBean testBean = bf.getBean(TestBean.class);
        testBean.test();
    }
 ```
不出意外，我们会看到控制台打印了如下代码:
>before1
beforeTest
test
afterTest
after1

&emsp;&emsp;Spring 实现了对所有类的test方法进行增强，使辅助功能可以独立于核心业务之外，方便与程序的扩展和解耦。
&emsp;&emsp;那么，Spring究竟是如何实现 AOP 的呢？ 首先我们知道，Spring是否支持注解的AOP是由一个配置文件控制的，也就是<aop:aspect-autoproxy />,当在配置文件中声明了这句配置的时候，Spring就会支持注解的AOP，那么我们的分析就从这句注解开始。

## 7.2 动态AOP自定义标签
&emsp;&emsp;之前讲过 Spring 中的自定义注解，如果声明了自定义的注解，那么就一定会在程序中的某个地方注册了对应的解析器。我们搜索整个代码，尝试找到注册的地方，全局搜索后我们发现了在 AopNamespaceHandler 中对应着这样一段函数：
 ```java
    @Override
	public void init() {
		// In 2.0 XSD as well as in 2.1 XSD.
		registerBeanDefinitionParser("config", new ConfigBeanDefinitionParser());
		registerBeanDefinitionParser("aspectj-autoproxy", new AspectJAutoProxyBeanDefinitionParser());
		registerBeanDefinitionDecorator("scoped-proxy", new ScopedProxyBeanDefinitionDecorator());

		// Only in 2.0 XSD: moved to context namespace as of 2.1
		registerBeanDefinitionParser("spring-configured", new SpringConfiguredBeanDefinitionParser());
	}
 ```
&emsp;&emsp;我们可以得知，在解析自定义配置的时候，一旦遇到 aspectj-autoproxy 注解时，就会使用解析器 AspectJAutoProxyBeanDefinitionParser 进行解析，那么我们来看看 AspectJAutoProxyBeanDefinitionParser 的内部实现。
### 7.2.1 注册 AnnotationAwareAspectJAutoProxyCreator
&emsp;&emsp;所有解析器，因为是对 BeanDefinitionParser 接口的统一实现，入口都是从 parse 函数开始的， AspectJAutoProxyBeanDefinitionParse 的 parse 函数如下：
 ```java
    @Override
	@Nullable
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		//注册 AnnotationAwareAspectJAutoProxyCreator
		AopNamespaceUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(parserContext, element);
		//主注解中子类的处理
		extendBeanDefinition(element, parserContext);
		return null;
	}
 ```
&emsp;&emsp;其中 registerAspectJAnnotationAutoProxyCreatorIfNecessary 函数是我们比较关心的，也是关键逻辑的实现。
 ```java
    /**
	 * 注册 AnnotationAwareAspectJAutoProxyCreator
	 * @param parserContext
	 * @param sourceElement
	 */
	public static void registerAspectJAnnotationAutoProxyCreatorIfNecessary(
			ParserContext parserContext, Element sourceElement) {

		//注册或升级 AutoProxyCreator 定义 beanName 为 org.spring.framework.aop.config.internalAutoProxyCreator 的 BeanDefinition
		BeanDefinition beanDefinition = AopConfigUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(
				parserContext.getRegistry(), parserContext.extractSource(sourceElement));
		//对于 proxy-target-class 以及 expose-proxy 属性的处理
		useClassProxyingIfNecessary(parserContext.getRegistry(), sourceElement);
		//注册组件并通知，便于监听器做进一步处理
		registerComponentIfNecessary(beanDefinition, parserContext);
	}
 ```
 &emsp;&emsp;在 registerAspectJAnnotationAutoProxyCreatorIfNecessary 方法中主要完成了3件事情，基本上每行代码就是一个完整的逻辑。
#### 1.注册或升级 AnnotationAwareAspectJAutoProxyCreator
&emsp;&emsp;对于 AOP 的实现，基本上都是靠 AnnotationAwareAspectJAutoProxyCreator 去完成，它可以根据 @Point 注解定义的切点来自动代理相匹配的bean。但是为了配置简便，Spring 使用了自定义配置来帮助我们自动注册 AnnotationAwareAspectJAutoProxyCreator，其注册过程就是在这里实现的。
 ```java
    @Nullable
	public static BeanDefinition registerAspectJAnnotationAutoProxyCreatorIfNecessary(
			BeanDefinitionRegistry registry, @Nullable Object source) {

		return registerOrEscalateApcAsRequired(AnnotationAwareAspectJAutoProxyCreator.class, registry, source);
	}
    @Nullable
	private static BeanDefinition registerOrEscalateApcAsRequired(
			Class<?> cls, BeanDefinitionRegistry registry, @Nullable Object source) {

		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		//如果已经存在了自动代理创建器且存在的自动代理创建器与现在的不一样，那么根据优先级来判断到底使用谁
		//AUTO_PROXY_CREATOR_BEAN_NAME = "org.springframework.aop.config.internalAutoProxyCreator"
		if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
			BeanDefinition apcDefinition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
			if (!cls.getName().equals(apcDefinition.getBeanClassName())) {
				int currentPriority = findPriorityForClass(apcDefinition.getBeanClassName());
				int requiredPriority = findPriorityForClass(cls);
				if (currentPriority < requiredPriority) {
					//改变 bean 最重要的就是改变 bean 所对应的 className 属性
					apcDefinition.setBeanClassName(cls.getName());
				}
			}
			//如果已经存在自动代理器且与将要创建的一致，那么无须再次创建
			return null;
		}

		RootBeanDefinition beanDefinition = new RootBeanDefinition(cls);
		beanDefinition.setSource(source);
		beanDefinition.getPropertyValues().add("order", Ordered.HIGHEST_PRECEDENCE);
		beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		registry.registerBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME, beanDefinition);
		return beanDefinition;
	}
 ```
&emsp;&emsp;以上代码中实现了自动注册 AnnotationAwareAspectJAutoProxyCreator 类的功能，同时这里还涉及了一个优先级的问题，如果已经存在了自动代理创建器，而且存在的自动代理创建器与现在的不一致，那么需要根据优先级来判断到底需要使用哪个。
#### 2.处理 proxy-target-class 以及 expose-proxy 属性
&emsp;&emsp;useClassProxyingIfNecessary 实现了 proxy-target-class 属性以及 expose-proxy 属性的处理。
 ```java
    private static void useClassProxyingIfNecessary(BeanDefinitionRegistry registry, @Nullable Element sourceElement) {
		if (sourceElement != null) {
			//对于 proxy-target-class 属性的处理
			boolean proxyTargetClass = Boolean.parseBoolean(sourceElement.getAttribute(PROXY_TARGET_CLASS_ATTRIBUTE));
			if (proxyTargetClass) {
				AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
			}
			// 对于 expose-proxy 属性的处理
			boolean exposeProxy = Boolean.parseBoolean(sourceElement.getAttribute(EXPOSE_PROXY_ATTRIBUTE));
			if (exposeProxy) {
				AopConfigUtils.forceAutoProxyCreatorToExposeProxy(registry);
			}
		}
	}
 ```
> AopConfigUtils
 ```java
    //强制使用的过程中其实也是一个属性设置的过程
    public static void forceAutoProxyCreatorToUseClassProxying(BeanDefinitionRegistry registry) {
		if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
			BeanDefinition definition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
			definition.getPropertyValues().add("proxyTargetClass", Boolean.TRUE);
		}
	}
    public static void forceAutoProxyCreatorToExposeProxy(BeanDefinitionRegistry registry) {
		if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
			BeanDefinition definition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
			definition.getPropertyValues().add("exposeProxy", Boolean.TRUE);
		}
	}
 ```
* proxy-target-class:Spring AOP 部分使用JDK动态代理或者CGLIB来为目标对象创建代理(建议尽量使用JDK动态代理)。如果被代理的目标对象实现了至少一个借口，则会使用JDK动态代理。所有该目标类型实现的接口都将被代理。若该目标对象没有实现任何接口，则创建一个CGLIB代理。如果你希望强制使用CGLIB代理(例如希望代理目标对象的所有方法，而不只是实现自接口的方法)，那也可以。但需要考虑一下两个问题。
1. 无法通知(advise)Final方法，因为它们不能被覆盖。
1. 你需要将CGLIB二进制发型包放在classpath下面。
与之相比，JDK本身就提供了动态代理，强制使用CGLIB代理需要将<aop:config>的proxy-target-class属性设置为true：
 ```xml
 <aop:config proxy-target-class="true">...</aop:config>
 ```
&emsp;&emsp;当然使用CGLIB代理啊和@AspectJ自动代理支持，可以按照一下方式设置<aop:aspectj-autoproxy>的proxy-target-class属性：
 ```xml
 <aop:aspectj-autoproxy proxy-target-class="true" />
 ```
&emsp;&emsp;而实际使用的过程中才会发现细节问题的差别，The devil is in the details。
* JDK动态代理：期待你对象必须是某个接口的实现，它是通过在运行期间创建一个接口的实现类来完成对目标对象的代理。
* CGLIB代理：实现原理类似JDK动态代理，只是它在运行期间生成的代理对象是针对目标类扩展的之类。CGLIB是高效的代码生成包，底层是依靠ASM(开源的Java字节码编辑类库)操作字节码实现的，性能比JDK强。
* expose-proxy：有时候目标对象内部的自我调用将无法实施切面中的增强，如下示例：
 ```java
 public interface AService{
     public void a();
     public void b();
 }
 @Service()
 public class AServiceImpl implements AService{
     @Transactional(propagation = Propagation.REQUIRED)
     public void a(){
         this.b();
     }
     @Transactional(propagation = Propagation.REQUIRED_NEW)
     public void b(){
     }
 }
 ```
&emsp;&emsp;此处的this指向目标对象，因此调用this.b()将不会执行b事务切面，即不会执行事务增强，因此b方法的事务定义 "@Transactional(propagation = Propagation.REQUIRED_NEW)" 将不会实施，为了解决这个问题，我们可以这样做：
 ```xml
 <aop:aspectj-autoproxy expose-proxy="true"/>
 ```
&emsp;&emsp;然后将以上代码中的 "this.b()" 修改为 "((AService)AopContext.currentProxy()).b();"即可。通过以上代码的修改便可以完成对a和b方法的同时增强。
&emsp;&emsp;最后注册组件并通知，便于监听器做进一处理。



