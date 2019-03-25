#Spring Boot 特性

##1.条件化配置 
&emsp;&emsp;在向应用程序加入Spring Boot时，有个名为spring-boot-autoconfigure的JAR文件，其中包含了很多配置类。每个配置类都在应用程序的Classpath里，都有机会为应用程序的配置添砖加瓦。这些配置类里有用于Thymeleaf的配置，有用于Spring Data JPA的配置，有用于Spiring MVC的配置，还有很多其他东西的配置，你可以自己选择是否在Spring应用程序里使用它们。所有这些配置如此与众不同，原因在于它们利用了Spring的条件化配置，这是Spring 4.0引入的新特性。条件化配置允许配置存在于应用程序中，但在满足某些特定条件之前都忽略这个配置。
&emsp;&emsp;在Spring里可以很方便地编写你自己的条件，你所要做的就是实现Condition接口，覆盖它的matches()方法。举例来说，下面这个简单的条件类只有在Classpath里存在JdbcTemplate时才会生效：
```java
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class JdbcTemplateCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context,AnnotatedTypeMetadata metadata) {
        try {
            context.getClassLoader().loadClass("org.springframework.jdbc.core.JdbcTemplate");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```
&emsp;&emsp;当你用Java来声明Bean的时候，可以使用这个自定义条件类：
```java
@Conditional(JdbcTemplateCondition.class)
public MyService myService() {
...
}
```

&emsp;&emsp;只有当JdbcTemplateCondition类的条件成立时才会创建MyService这个Bean。也就是说MyService Bean创建的条件是Classpath里有JdbcTemplate。否则，这个Bean的声明就会被忽略掉。

&emsp;&emsp;Spring Boot定义了很多更有趣的条件，并把它们运用到了配置类上，这些配置类构成了Spring Boot的自动配置。Spring Boot运用条件化配置的方法是，定义多个特殊的条件化注解，并将它们用到配置类上

**<center>自动配置中使用的条件化注解</center>**
| 条件话注解 | 配置生效条件 |
| :------| :------|
|@ConditionalOnBean|配置了某个特定Bean|
|@ConditionalOnMissBean|没有配置特定的Bean|
|@ConditionalOnClass|Classpath里有指定的类|
|@ConditionalOnClass|Classpath里缺少指定的类|
|@CondionalOnExpression|给定的Spring Expression(SpEl)表达式计算结果为true|
|@CondionalOnJava|Java的版本匹配特定值或者一个范围|
|@CondionalOnJndi|参数中给定的JNDI位置必须存在一个，如果没有给定参数，则要JNDIInitialContext|
|@CondionalOnProperty|指定的配置属性要有一个明确的值|
|@CondionalOnResource|Classpath有指定的资源|
|@CondionalOnWebApplication|这是一个Web应用|
|@CondionalOnNotWebApplication|这不是一个Web应用|

 &emsp;&emsp;一般来说，无需查看Spring Boot自动配置类的源代码，但为了演示如何使用上表里的注解，我们可以看一下DataSourceAutoConfiguration里的这个片段（这是Spring Boot自动配置库的一部分）：
```java
@Configuration
@ConditionalOnClass({ DataSource.class, EmbeddedDatabaseType.class })
@EnableConfigurationProperties(DataSourceProperties.class)
@Import({ Registrar.class, DataSourcePoolMetadataProvidersConfiguration.class })
public class DataSourceAutoConfiguration {
...
}
```
&emsp;&emsp;如你所见，DataSourceAutoConfiguration添加了@Configuration注解，它从其他配置类里导入了一些额外配置，还自己定义了一些Bean。最重要的是，DataSourceAutoConfiguration上添加了@ConditionalOnClass注解，要求Classpath里必须要有DataSource和EmbeddedDatabaseType。如果它们不存在，条件就不成立，DataSourceAutoConfiguration提供的配置都会被忽略掉。DataSourceAutoConfiguration里嵌入了一个JdbcTemplateConfiguration类，自动配置了一个JdbcTemplate Bean：
```java
@Configuration
@Conditional(DataSourceAutoConfiguration.DataSourceAvailableCondition.class)
protected static class JdbcTemplateConfiguration {
    @Autowired(required = false)
    private DataSource dataSource;
    @Bean
    @ConditionalOnMissingBean(JdbcOperations.class)
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(this.dataSource);
    }
...
}
```
&emsp;&emsp;JdbcTemplateConfiguration使用了@Conditional注解，判断DataSourceAvailable-Condition条件是否成立——基本上就是要有一个DataSource Bean或者要自动配置创建一个。

&emsp;&emsp;假设有DataSource Bean ， 使用了@Bean 注解的jdbcTemplate()方法会配置一个JdbcTemplate Bean。这个方法上还加了@ConditionalOnMissingBean注解，因此只有在不存在JdbcOperations（即JdbcTemplate实现的接口）类型的Bean时，才会创建JdbcTemplateBean。此处看到的只是DataSourceAutoConfiguration的冰山一角，Spring Boot提供的其他自动配置类也有很多知识没有提到。但这已经足以说明Spring Boot如何利用条件化配置实现自动配置。

&emsp;&emsp;自动配置会做出以下配置决策，它们和之前的例子息息相关。
1. 因为Classpath 里有H2 ， 所以会创建一个嵌入式的H2 数据库Bean ， 它的类型是javax.sql.DataSource，JPA实现（Hibernate）需要它来访问数据库。
1. 因为Classpath里有Hibernate（Spring Data JPA传递引入的）的实体管理器，所以自动配置会配置与Hibernate 相关的Bean ， 包括Spring 的LocalContainerEntityManager-FactoryBean和JpaVendorAdapter。
1. 因为Classpath里有Spring Data JPA，所以它会自动配置为根据仓库的接口创建仓库实现。
1. 因为Classpath里有Thymeleaf，所以Thymeleaf会配置为Spring MVC的视图，包括一个Thymeleaf的模板解析器、模板引擎及视图解析器。视图解析器会解析相对于Classpath根目录的/templates目录里的模板。
1. 因为Classpath 里有Spring MVC （ 归功于Web 起步依赖）， 所以会配置Spring 的并启用Spring MVC。
1. 因为这是一个Spring MVC Web应用程序，所以会注册一个资源处理器，把相对于Classpath根目录的/static目录里的静态内容提供出来。（这个资源处理器还能处理/public、/resources和/META-INF/resources的静态内容。）
1. 因为Classpath里有Tomcat（通过Web起步依赖传递引用），所以会启动一个嵌入式的Tomcat容器，监听8080端口。

&emsp;&emsp;由此可见，Spring Boot自动配置承担起了配置Spring的重任，因此你能专注于编写自己的应用程序。