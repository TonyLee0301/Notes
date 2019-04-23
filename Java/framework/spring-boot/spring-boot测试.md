
<!-- @import "[TOC]" {cmd="toc" depthFrom=1 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->

* [spring-boot 集成测试自动配置](#spring-boot-集成测试自动配置)
* [测试Web应用程序](#测试web应用程序)
	* [模式 Spring MVC](#模式-spring-mvc)
	* [测试Web安全](#测试web安全)
* [测试运行中的应用程序](#测试运行中的应用程序)

<!-- /code_chunk_output -->

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

# 测试Web应用程序
Spring MVC有一个优点:它的编程模型是围绕POJO展开的，在POJO上添加注解，声明如何 处理Web请求。这种编程模型不仅简单，还让你能像对待应用程序中的其他组件一样对待这些控 制器。你还可以针对这些控制器编写测试，就像测试POJO一样。
举例来说，考虑ReadingListController里的addToReadingList()方法:
```java
    @RequestMapping(method=RequestMethod.POST)
    public String addToReadingList(Book book) {
      book.setReader(reader);
      readingListRepository.save(book);
      return "redirect:/readingList";
    }
```
如果忽略@RequestMapping注解，你得到的就是一个相当基础的Java方法。你立马就能想到这样一个测试，提供一个ReadingListRepository的模拟实现，直接调用addToReadingList()，判断返回值并验证对ReadingListRepository的save()方法有过调用。

该测试的问题在于，它仅仅测试了方法本身，当然，这要比没有测试好一点。然而，它没有测试该方法处理/readingList的POST请求的情况，也没有测试表单域绑定到Book参数的情况。虽然你可以判断返回的String包含特定值，但没法明确测试请求在方法处理完之后是否真的会重 定向到/readingList。

要恰当地测试一个Web应用程序，你需要投入一些实际的HTTP请求，确认它能正确地处理那些请求。幸运的是，Spring Boot开发者有两个可选的方案能实现这类测试。

* Spring Mock MVC:能在一个近似真实的模拟Servlet容器里测试控制器，而不用实际启动 应用服务器。
* Web集成测试:在嵌入式Servlet容器(比如Tomcat或Jetty)里启动应用程序，在真正的应 用服务器里执行测试。

这两种方法各有利弊。很明显，启动一个应用服务器会比模拟Servlet容器要慢一些，但毫无疑问基于服务器的测试会更接近真实环境，更接近部署到生产环境运行的情况。
接下来，你会看到如何使用Spring Mock MVC测试框架来测试Web应用程序。

##模式 Spring MVC
早在Spring 3.2，Spring Framework就有了一套非常实用的Web应用程序测试工具，能模拟Spring MVC，不需要真实的Servlet容器也能对控制器发送HTTP请求。Spring的Mock MVC框架模拟了Spring MVC的很多功能。它几乎和运行在Servlet容器里的应用程序一样，尽管实际情况并非如此。
要在测试里设置Mock MVC，可以使用MockMvcBuilders，该类提供了两个静态方法。
* standaloneSetup():构建一个Mock MVC，提供一个或多个手工创建并配置的控制器。 
* webAppContextSetup():使用Spring应用程序上下文来构建Mock MVC，该上下文里可以包含一个或多个配置好的控制器。

两者的主要区别在于，standaloneSetup()希望你手工初始化并注入你要测试的控制器，而webAppContextSetup()则基于一个WebApplicationContext的实例，通常由Spring加载。 前者同单元测试更加接近，你可能只想让它专注于单一控制器的测试，而后者让Spring加载控制 器及其依赖，以便进行完整的集成测试。

我们要用的是webAppContextSetup()。Spring完成了ReadingListController的初始化，并从Spring Boot自动配置的应用程序上下文里将其注入，我们直接对其进行测试。

webAppContextSetup()接受一个WebApplicationContext参数。因此，我们需要为测试类加上@WebAppConfiguration注解，使用@Autowired将WebApplicationContext作为实 例变量注入测试类。代码清单4-2演示了Mock MVC测试的执行入口。

>代码清单4-2 为集成测试控制器创建Mock MVC
```java
@RunWith(SpringJUnit4ClassRunner.class)
//@SpringApplicationConfiguration(classes = ReadingListApplication.class) 该注解，在新版本中已经去掉，直接使用@SpringBootTest
@SpringBootTest
@WebAppConfiguration
public class MockMvcWebTest {

    @Autowired
    private WebApplicationContext webContext;

    private MockMvc mockMvc;

    @Before
    public void setupMockMvc(){
        mockMvc = MockMvcBuilders.webAppContextSetup(webContext).build()
    }

}
```
@WebAppConfiguration注解声明，由SpringJUnit4ClassRunner创建的应用程序上下文应该是一个WebApplicationContext(相对于基本的非WebApplicationContext)。

setupMockMvc()方法上添加了JUnit的@Before注解，表明它应该在测试方法之前执行。它将WebApplicationContext注入webAppContextSetup()方法，然后调用build()产生了一 个MockMvc实例，该实例赋给了一个实例变量，供测试方法使用。

现在我们有了一个MockMvc，已经可以开始写测试方法了。我们先写个简单的测试方法，向 /readingList发送一个HTTP GET请求，判断模型和视图是否满足我们的期望。下面的homePage() 测试方法就是我们所需要的:

```java
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@Test
public void homePage() throws Exception {
    mockMvc.perform(get("/readingList")) //发起get请求
        .andExpect(status().isOk()) //期望http state 返回值 200
        .andExpect(view().name("readingList")) //期望返回的视图
        .andExpect(model().attributeExists("books")) //期望返回的值，存在books
        .andExpect(model().attribute("books", is(empty()))); //期望返回的 boos 为空
}
```
首先向/readingList发起一个GET请求，接下来希望该请求处理成功(isOk()会判断HTTP 200响应码)，并且视图的逻辑名称为readingList。测试 还要断定模型包含一个名为books的属性，该属性是一个空集合。所有的断言都很直观。

值得一提的是，此处完全不需要将应用程序部署到Web服务器上，它是运行在模拟的Spring MVC中的，刚好能通过MockMvc实例处理我们给它的HTTP请求。

我们实际发送一个HTTP POST请求提交一本新书。我们应该期待POST请求处理后重定向回/readingList，模型将包含新添加的图书。代码清单4-3 演示了如何通过Spring的Mock MVC来实现这个测试。
>代码清单4-3 演示了如何通过Spring的Mock MVC来实现这个测试
```java
@Test
public void postBook() throws Exception {
    mockMvc.perform(post("readingList")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("title", "BOOK TITLE")
                .param("author", "BOOK AUTHOR")
                .param("isbn", "1234567890")
                .param("description", "DESCRIPTION")
        ).andExpect(status().is3xxRedirection()).andExpect(header().string("Location", "/readingList"));

    Book expectedBook = new Book();
    expectedBook.setId(1L);
    expectedBook.setReader("craig");
    expectedBook.setTitle("BOOK TITLE");
    expectedBook.setAuthor("BOOK AUTHOR");
    expectedBook.setIsbn("1234567890");
    expectedBook.setDescription("DESCRIPTION");

    mockMvc.perform(get("/readingList"))
            .andExpect(status().isOk())
            .andExpect(view().name("readingList"))
            .andExpect(model().attributeExists("books"))
            .andExpect(model().attribute("books", hasSize(1)))
            .andExpect(model().attribute("books", contains(samePropertyValuesAs(expectedBook))));
}
```

##测试Web安全
Spring Security能让你非常方便地测试安全加固后的Web应用程序。为了利用这点优势，你必 须在项目里添加Spring Security的测试模块。要在Gradle里做到这一点，你需要的就是以下 testCompile依赖:
```gradle
testCompile("org.springframework.security:spring-security-test") 
```
如果你用的是Maven，则添加以下
```xml
<dependency>:
    <dependency>
      <groupId>org.springframework.security</groupId>
      <artifactId>spring-security-test</artifactId>
      <scope>test</scope>
</dependency>
```
应用程序的Classpath里有了Spring Security的测试模块之后，只需在创建MockMvc实例时运用
Spring Security的配置器。
```java
@Before
public void setupMockMvc() {
    mockMvc = MockMvcBuilders
        .webAppContextSetup(webContext)
        .apply(springSecurity())
        .build();
}
```
springSecurity()方法返回了一个Mock MVC配置器，为Mock MVC开启了Spring Security 支持。只需像上面这样运用就行了，Spring Security会介入MockMvc上执行的每个请求。具体的安全配置取决于你如何配置Spring Security(或者Spring Boot如何自动配置Spring Security)。

**springSecurity()** 方法 springSecurity()是SecurityMockMvcConfigurers的一个静态方法，**考虑到可读性，我已经将其态导入。** 

开启了Spring Security之后，在请求主页的时候，我们便不能只期待HTTP 200响应。如果请 求未经身份验证，我们应该期待重定向到登录页面:
```java
@Test
public void homePage_unauthenticatedUser() throws Exception {
mockMvc.perform(get("/"))
    .andExpect(status().is3xxRedirection())
    .andExpect(header().string("Location",
                               "http://localhost/login"));
}
```
不过，经过身份验证的请求又该如何发起呢?Spring Security提供了两个注解。
* @WithMockUser:加载安全上下文，其中包含一个UserDetails，使用了给定的用户名、密码和授权。
* @WithUserDetails:根据给定的用户名查找UserDetails对象，加载安全上下文。

在这两种情况下，Spring Security的安全上下文都会加载一个UserDetails对象，添加了该注解的测试方法在运行过程中都会使用该对象。@WithMockUser注解是两者里比较基础的那个， 允许显式声明一个UserDetails，并加载到安全上下文。
```java
@Test
@WithMockUser(username="craig",
              password="password",
              roles="READER")
public void homePage_authenticatedUser() throws Exception {
... }
```
如你所见，@WithMockUser绕过了对UserDetails对象的正常查询，用给定的值创建了一 个UserDetails对象取而代之。在简单的测试里，这就够用了。但我们的测试需要Reader(实现了UserDetails)而非@WithMockUser创建的通用UserDetails。为此，我们需要 @WithUserDetails。

@WithUserDetails注解使用事先配置好的UserDetailsService来加载UserDetails对象。所以我们要为测试方法添加@WithUserDetails注解，如 代码清单4-4所示。
>代码清单4-4
```java
@Test
@WithUserDetails("craig")
public void homePage_authenticatedUser() throws Exception {
    Reader expectedReader = new Reader();
    expectedReader.setUsername("craig");
    expectedReader.setPassword("password");
    expectedReader.setFullname("Craig Walls");

    mockMvc.perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(view().name("readingList"))
        .andExpect(model().attribute("reader",samePropertyValuesAs(expectedReader)))
        .andExpect(model().attribute("books", hasSize(0)));
}
```
在代码清单4-4里，我们通过@WithUserDetails注解声明要在测试方法执行过程中向安全 上下文里加载craig用户。Reader会放入模型，该测试方法先创建了一个期望的Reader对象，后 续可以用来进行比较。随后GET请求发起，也有了针对视图名和模型内容的断言，其中包括名为 reader的模型属性。

同样，此处没有启动Servlet容器来运行这些测试，Spring的Mock MVC取代了实际的Servlet 容器。这样做的好处是测试方法运行相对较快。因为不需要等待服务器启动，而且不需要打开 Web浏览器发送表单，所以测试比较简单快捷。

不过，这并不是一个完整的测试。它比直接调用控制器方法要好，但它并没有真的在Web浏览器里执行应用程序，验证呈现出的视图。为此，我们需要启动一个真正的Web服务器，用真实浏览器来访问它。让我们来看看Spring Boot如何启动一个真实的Web服务器来帮助测试。

#测试运行中的应用程序
说到测试Web应用程序，我们还没接触实质内容。在真实的服务器里启动应用程序，用真实的Web浏览器访问它，这样比使用模拟的测试引擎更能展现应用程序在用户端的行为。

但是，用真实的Web浏览器在真实的服务器上运行测试会很麻烦。虽然构建时的插件能把应用程序部署到Tomcat或者Jetty里，但它们配置起来多有不便。而且测试这么多，几乎不可能隔离运行，也很难不启动构建工具。

然而Spring Boot找到了解决方案。它支持将Tomcat或Jetty这样的嵌入式Servlet容器作为运行 中的应用程序的一部分，可以运用相同的机制，在测试过程中用嵌入式Servlet容器来启动应用 程序。

Spring Boot的@WebIntegrationTest注解就是这么做的。在测试类上添加@Web- IntegrationTest注解，可以声明你不仅希望Spring Boot为测试创建应用程序上下文，还要启 动一个嵌入式的Servlet容器。一旦应用程序运行在嵌入式容器里，你就可以发起真实的HTTP请求，断言结果了。

举例来说，考虑一下代码清单4-5里的那段简单的Web测试。这里采用@WebIntegration- Test，在服务器里启动了应用程序，以Spring的RestTemplate对应用程序发起HTTP请求。

>代码清单4-5 测试运行在服务器里的Web应用程序
```java
@RunWith(SpringJUnit4ClassRunner.class)
//@SpringApplicationConfiguration(classes=ReadingListApplication.class)
@SpringBootTest
@WebIntegrationTest
public class SimpleWebTest {
    @Test(expected=HttpClientErrorException.class)
    public void pageNotFound() {
        try {
            RestTemplate rest = new RestTemplate();
            rest.getForObject("http://localhost:8080/bogusPage", String.class);
            fail("Should result in HTTP 404");
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode()); 
            throw e;
        }
    }
}
```