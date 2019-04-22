# spring-boot 集成测试自动配置
Spring Framework的核心工作是将所有组件编织在一起，构成一个应用程序。整个过程就是读取配置说明(可以是XML、基于Java的配置、基于Groovy的配置或其他类型的配置)，在应用程序上下文里初始化Bean，将Bean注入依赖它们的其他Bean中。 对Spring应用程序进行集成测试时，让Spring遵照生产环境来组装测试目标Bean是非常重要的一点。当然，你也可以手动初始化组件，并将它们注入其他组件，但对那些大型应用程序来说， 这是项费时费力的工作。而且，Spring提供了额外的辅助功能，比如组件扫描、自动织入和声明 性切面(缓存、事务和安全，等等)。你要把这些活都干了，基本也就是把Spring再造了一次，最 好还是让Spring替你把重活都做了吧，哪怕是在集成测试里。
Spring自1.1.1版就向集成测试提供了极佳的支持。自Spring 2.5开始，集成测试支持的形式就 变成了SpringJUnit4ClassRunner。这是一个JUnit类运行器，会为JUnit测试加载Spring应用程 序上下文，并为测试类自动织入所需的Bean。
举例来说，看一下代码清单1，这是一个非常基本的Spring集成测试。
>代码清单1 用SpringJUnit4ClassRunner对Spring应用程序进行集成测试
```java 
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes=AddressBookConfiguration.class)
public class AddressServiceTests {
    @Autowired
    private AddressService addressService;
    @Test
    public void testService() {
        Address address = addressService.findByLastName("Sheman"); assertEquals("P", address.getFirstName()); 
        assertEquals("Sherman", address.getLastName()); 
        assertEquals("42 Wallaby Way", address.getAddressLine1()); assertEquals("Sydney", address.getCity()); 
        assertEquals("New South Wales", address.getState()); assertEquals("2000", address.getPostCode());
    } 
}
``` 
如你所见，AddressServiceTests上加注了`@RunWith`和`@ContextConfiguration`注解。 `@RunWith`的参数是`SpringJUnit4ClassRunner.class`，开启了Spring集成测试支持。与此同时，`@ContextConfiguration`指定了如何加载应用程序上下文。此处我们让它加载`AddressBookConfiguration`里配置的Spring应用程序上下文。
除了加载应用程序上下文，`SpringJUnit4ClassRunner`还能通过自动织入从应用程序上下文里向测试本身注入Bean。因为这是一个针对AddressService Bean的测试，所以需要将它注入测试。最后，testService()方法调用地址服务并验证了结果。

虽然@ContextConfiguration在加载Spring应用程序上下文的过程中做了很多事情，但它没能加载完整的Spring Boot。Spring Boot应用程序最终是由SpringApplication加载的。它可以显式加载(如代码清单2所示)，在这里也可以使用SpringBootServletInitializer。SpringApplication不仅加载应用程序上下文，还会开启日志、 加载外部属性(application.properties或application.yml)，以及其他Spring Boot特性。用@ContextConfiguration则得不到这些特性。
要在集成测试里获得这些特性，可以把@ContextConfiguration替换为Spring Boot的 @SpringApplicationConfiguration:
```java 
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes=AddressBookConfiguration.class)
public class AddressServiceTests {
    ...
}
``` 
`@SpringApplicationConfiguration`的用法和`@ContextConfiguration`大致相同，但也有不同的地方，@SpringApplicationConfiguration加载Spring应用程序上下文的方式同 `SpringApplication`相同，处理方式和生产应用程序中的情况相同。这包括加载外部属性和 `Spring Boot`日志。

我们有充分的理由说，在大多数情况下，为Spring Boot应用程序编写测试时应该用@SpringApplicationConfiguration代替@ContextConfiguration。

