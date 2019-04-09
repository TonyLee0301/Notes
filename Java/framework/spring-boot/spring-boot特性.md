<!-- @import "[TOC]" {cmd="toc" depthFrom=1 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->

* [Spring Boot 特性](#spring-boot-特性)
	* [1. 条件化配置](#1-条件化配置)
	* [2. 自动化配置](#2-自动化配置)
		* [2.1、通过属性文件外置配置](#21-通过属性文件外置配置)
			* [自动配置微调](#自动配置微调)
				* [1. 禁用模板缓存](#1-禁用模板缓存)
				* [2. 配置嵌入式服务器](#2-配置嵌入式服务器)
				* [3. 配置日志](#3-配置日志)
				* [4. 配置数据源](#4-配置数据源)
		* [2.2、应用程序 Bean 的配置外置](#22-应用程序-bean-的配置外置)
		* [2.3、使用Profile进行配置](#23-使用profile进行配置)
			* [1.使用特定于Profile的属性文件](#1使用特定于profile的属性文件)
			* [2. 使用多Profile YAML文件进行配置](#2-使用多profile-yaml文件进行配置)
		* [2.4、定制应用程序错误页面](#24-定制应用程序错误页面)
	* [3. 小结](#3-小结)

<!-- /code_chunk_output -->

#Spring Boot 特性

##1. 条件化配置 
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

**<center><span id="table-1">自动配置中使用的条件化注解</span></center>**
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
<center>表1</center>
          
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

##2. 自动化配置
&emsp;&emsp;Spring Boot自动配置自带了很多配置类，每一个都能运用 在你的应用程序里。它们都使用了Spring 4.0的条件化配置，可以在运行时判断这个配置是该被运 用，还是该被忽略。
[表1](#table-1)中的@ConditionalOnMissingBean注解是覆盖自动配置的关键。 Spring Boot的DataSourceAutoConfiguration中定义的JdbcTemplate Bean就是一个非常简 单的例子，演示了@ConditionalOnMissingBean如何工作:
```java
    @Bean
    @ConditionalOnMissingBean(JdbcOperations.class)
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(this.dataSource);
    }
```
jdbcTemplate()方法上添加了@Bean注解，在需要时可以配置出一个JdbcTemplate Bean。但它上面还加了@ConditionalOnMissingBean注解，要求当前不存在JdbcOperations 类型(JdbcTemplate实现了该接口)的Bean时才生效。如果当前已经有一个JdbcOperations Bean了，条件即不满足，不会执行jdbcTemplate()方法。
什么情况下会存在一个JdbcOperations Bean呢?Spring Boot的设计是加载应用级配置，随 后再考虑自动配置类。因此，如果你已经配置了一个JdbcTemplate Bean，那么在执行自动配置 时就已经存在一个JdbcOperations类型的Bean了，于是忽略自动配置的JdbcTemplate Bean。

### 2.1、通过属性文件外置配置
在处理应用安全时，你当然会希望完全掌控所有配置。不过，为了微调一些细节，比如改改 端口号和日志级别，便放弃自动配置，这是一件让人羞愧的事。为了设置数据库URL，是配置一 个属性简单，还是完整地声明一个数据源的Bean简单?答案不言自明，不是吗?
事实上，Spring Boot自动配置的Bean提供了300多个用于微调的属性。当你调整设置时，只 要在环境变量、Java系统属性、JNDI(Java Naming and Directory Interface)、命令行参数或者属 性文件里进行指定就好了。
要了解这些属性，让我们来看个非常简单的例子。你也许已经注意到了，在命令行里运行阅 读列表应用程序时，Spring Boot有一个`ascii-art Banner`。如果你想禁用这个Banner，可以将 `spring.main.show-banner`属性设置为false。有几种实现方式，其中之一就是在运行应用程 序的命令行参数里指定:
```shell
$ java -jar xxx.jar --spring.main.show-banner=false
```
另一种方式是创建一个名为application.properties的文件，包含如下内容:
```properties
spring.main.show-banner=false
```
或者，如果你喜欢的话，也可以创建名为application.yml的YAML文件，内容如下:
```yaml
spring:
    main:
        show-banner: false
```
还可以将属性设置为环境变量。举例来说，如果你用的是bash或者zsh，可以用export命令:
```shell 
$ export spring_main_show_banner=false
``` 
请注意，这里用的是下划线而不是点和横杠，这是对环境变量名称的要求。
实际上，Spring Boot应用程序有多种设置途径。Spring Boot能从多种属性源获得属性，包括如下几处。
1. 命令行参数
1. java:comp/env里的JNDI属性
1. JVM系统属性
1. 操作系统环境变量
1. 随机生成的带random.*前缀的属性(在设置其他属性时，可以引用它们，比如${random. long})
1. 应用程序以外的application.properties或者appliaction.yml文件
1. 打包在应用程序内的application.properties或者appliaction.yml文件
1. 通过@PropertySource标注的属性源
1. 默认属性

这个列表按照优先级排序，也就是说，任何在高优先级属性源里设置的属性都会覆盖低优先
级的相同属性。例如，命令行参数会覆盖其他属性源里的属性。
application.properties和application.yml文件能放在以下四个位置。
1. 外置，在相对于应用程序运行目录的/config子目录里。
1. 外置，在应用程序运行的目录里。
1. 内置，在config包内。
1. 内置，在Classpath根目录。 同样，这个列表按照优先级排序。也就是说，/config子目录里的application.properties会覆盖

应用程序Classpath里的application.properties中的相同属性。 此外，如果你在同一优先级位置同时有application.properties和application.yml，那么application.
yml里的属性会覆盖application.properties里的属性。
禁用ascii-art Banner只是使用属性的一个小例子。让我们再看几个例子，看看如何通过常用途径微调自动配置的Bean。

#### 自动配置微调
如上所说，有300多个属性可以用来微调Spring Boot应用程序里的Bean。附录C有一个详尽的列表。此处无法逐一描述它们的细节，因此我们就通过几个例子来了解一些Spring Boot暴露的实用属性。
##### 1. 禁用模板缓存
如果阅读列表应用程序经过了几番修改，你一定已经注意到了，除非重启应用程序，否则对 Thymeleaf模板的变更是不会生效的。这是因为Thymeleaf模板默认缓存。这有助于改善应用程序 的性能，因为模板只需编译一次，但在开发过程中就不能实时看到变更的效果了。
将spring.thymeleaf.cache设置为false就能禁用Thymeleaf模板缓存。在命令行里运行 应用程序时，将其设置为命令行参数即可:
```shell
$ java -jar readinglist-0.0.1-SNAPSHOT.jar --spring.thymeleaf.cache=false
```
或者，如果你希望每次运行时都禁用缓存，可以创建一个application.yml，包含以下内容:
```yaml 
spring:
    thymeleaf:
        cache: false
``` 
你一定要确保这个文件不会发布到生产环境，否则生产环境里的应用程序就无法享受模板缓存带来的性能提升了。
作为开发者，在修改模板时始终关闭缓存实在太方便了。为此，可以通过环境变量来禁用 Thymeleaf缓存
```shell
$ export spring_thymeleaf_cache=false
```
此处使用Thymeleaf作为应用程序的视图，Spring Boot支持的其他模板也能关闭模板缓存，设置这些属性就好了:
* spring.freemarker.cache(Freemarker)
* spring.groovy.template.cache(Groovy模板) 
* spring.velocity.cache(Velocity)

默认情况下，这些属性都为true，也就是开启缓存。将它们设置为false即可禁用缓存。

##### 2. 配置嵌入式服务器
从命令行(或者Spring Tool Suite)运行Spring Boot应用程序时，应用程序会启动一个嵌入式
的服务器(默认是Tomcat)，监听8080端口。
无论出于什么原因，让服务器监听不同的端口，你所要做的就是设置server.port属性
```shell
#cmd
--server.port=8000
#yml
server:
    port: 8000
```

除了服务器的端口，你还可能希望服务器提供HTTPS服务。为此，第一步就是用JDK的 keytool工具来创建一个密钥存储(keystore):
```shell
$ keytool -keystore mykeys.jks -genkey -alias tomcat -keyalg RSA
```
该工具会询问几个与名字和组织相关的问题，大部分都无关紧要。但在被问到密码时，一定 要记住你的选择。在本例中，我选择letmein作为密码。
现在只需要设置几个属性就能开启嵌入式服务器的HTTPS服务了。可以把它们都配置在命令 行里，但这样太不方便了。可以把它们放在application.properties或application.yml里。在 application.yml中，它们可能是这样的:
```yml
server:
  port: 8443
  ssl:
    key-store: file:///path/to/mykeys.jks
    key-store-password: letmein
    key-password: letmein
```
此处的server.port设置为8443，开发环境的HTTPS服务器大多会选这个端口。 server.ssl.key-store属性指向密钥存储文件的存放路径。这里用了一个file://开头的URL， 从文件系统里加载该文件。你也可以把它打包在应用程序的JAR文件里，用classpath: URL来用它。server.ssl.key-store-password和server.ssl.key-password设置为创建该文件时给定的密码。
有了这些属性，应用程序就能在8443端口上监听HTTPS请求了。(根据你所用的浏览器，可 能会出现警告框提示该服务器无法验证其身份。在开发时，访问的是localhost，这没什么好担心的。)

##### 3. 配置日志
大多数应用程序都提供了某种形式的日志。即使你的应用程序不会直接记录日志，你所用的库也会记录它们的活动。
默认情况下，Spring Boot会用Logback(http://logback.qos.ch)来记录日志，并用INFO级别输 出到控制台。在运行应用程序和其他例子时，你应该已经看到很多INFO级别的日志了。
```
用其他日志实现替换Logback 一般来说，你不需要切换日志实现;Logback能很好地满足你的需要。但是，如果决定使
用Log4j或者Log4j2，那么你只需要修改依赖，引入对应该日志实现的起步依赖，同时排除掉 Logback。
以Maven为例，应排除掉根起步依赖传递引入的默认日志起步依赖，这样就能排除 Logback了:
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter</artifactId>
  <exclusions>
    <exclusion> 
        <groupId>org.springframework.boot</groupId> <artifactId>spring-boot-starter-logging</artifactId>
    </exclusion>
  </exclusions>
</dependency>
在Gradle里，在configurations下排除该起步依赖是最简单的办法:
configurations {
  all*.exclude group:'org.springframework.boot',
module:'spring-boot-starter-logging'
}
排除默认日志的起步依赖后，就可以引入你想用的日志实现的起步依赖了。在Maven里可 以这样添加Log4j:
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-log4j</artifactId>
</dependency>
在Gradle里可以这样添加Log4j: 
compile("org.springframework.boot:spring-boot-starter-log4j")
```

如果你想用Log4j2，可以把spring-boot-starter-log4j改成spring-boot-starter-log4j2。
要完全掌握日志配置，可以在Classpath的根目录(src/main/resources)里创建logback.xml文 件。下面是一个logback.xml的简单例子:
```xml
<configuration>
<appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
        <pattern>
            %d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
        </pattern>
    </encoder>
  </appender>
  <logger name="root" level="INFO"/>
  <root level="INFO">
    <appender-ref ref="STDOUT" />
  </root>
</configuration>
```
除了日志格式之外，这个Logback配置和不加logback.xml文件的默认配置差不多。但是，通 过编辑logback.xml，你可以完全掌控应用程序的日志文件。哪些配置应该放进logback.xml这个话 题不在本书的讨论范围内，请参考Logback的文档以了解更多信息。
即使如此，你对日志配置最常做的改动就是修改日志级别和指定日志输出的文件。使用了 Spring Boot的配置属性后，你可以在不创建logback.xml文件的情况下修改那些配置。
要设置日志级别，你可以创建以logging.level开头的属性，后面是要日志名称。如果根 日志级别要设置为WARN，但Spring Security的日志要用DEBUG级别，可以在application.yml里加入6以下内容:
```yml 
logging:
    level:
    root: WARN
    org:
        springframework:
            security: DEBUG
``` 
另外，你也可以把Spring Security的包名写成一行:  
```yml 
logging:
    level:
    root: WARN
    org.springframework.security: DEBUG
``` 
现在，假设你想把日志写到位于/var/logs/目录里的BookWorm.log文件里。使用logging.
path和loggin.file属性就行了:
```yml
logging:
    path: /var/logs/
    file: BookWorm.log
    level:
        root: WARN
        org:
            springframework:
                security: DEBUG
```
假设应用程序有/var/logs/的写权限，日志就能被写入/var/logs/BookWorm.log。默认情况下， 日志文件的大小达到10MB时会切分一次。
与之类似，这些属性也能在application.properties里设置:
```properties
logging.path=/var/logs/
logging.file=BookWorm.log
logging.level.root=WARN logging.level.root.org.springframework.security=DEBUG
```
如果你还是想要完全掌控日志配置，但是又不想用logback.xml作为Logback配置的名字，可 以通过logging.config属性指定自定义的名字:
```yml 
logging:
    config:
    classpath:logging-config.xml
``` 
虽然一般并不需要改变配置文件的名字，但是如果你想针对不同运行时Profile使用不同的日 志配置，这个功能会很有用。

##### 4. 配置数据源
用的是MySQL数据库，你的 application.yml文件看起来可能是这样的:
```yml
spring:
  datasource:
    url: jdbc:mysql://localhost/readinglist
    username: dbuser
    password: dbpass
```
通常你都无需指定JDBC驱动，Spring Boot会根据数据库URL识别出需要的驱动，但如果识 别出问题了，你还可以设置spring.datasource.driver-class-name属性:
```yml
spring:
  datasource:
  url: jdbc:mysql://localhost/readinglist
  username: dbuser
  password: dbpass
  driver-class-name: com.mysql.jdbc.Driver
``` 
在自动配置DataSource Bean的时候，Spring Boot会使用这里的连接数据。DataSource Bean是一个连接池，如果Classpath里有Tomcat的连接池DataSource，那么就会使用这个连接池; 否则，Spring Boot会在Classpath里查找以下连接池:
* HikariCP
* Commons DBCP
* Commons DBCP 2

这里列出的只是自动配置支持的连接池，你还可以自己配置DataSource Bean，使用你喜欢 的各种连接池。

你也可以设置spring.datasource.jndi-name属性，从JNDI里查找DataSource:
```yml 
spring:
  datasource:
    jndi-name: java:/comp/env/jdbc/readingListDS
``` 
一旦设置了spring.datasource.jndi-name属性，其他数据源连接属性都会被忽略，除 非没有设置别的数据源连接属性。
有很多影响Spring Boot自动配置组件的方法，只需设置一两个属性即可。但这种配置外置的
方法并不局限于Spring Boot配置的Bean。让我们看看如何使用这种属性配置机制来微调自己的应用程序组件。

### 2.2、应用程序 Bean 的配置外置
即常用的属性配置
开启配置属性从技术上来说，`@ConfigurationProperties`注解不会生效，除非先向Spring配置类添加`@EnableConfigurationProperties`注解。但通常无需这么做，因为Spring Boot自动配置后面的全部配置类都已经加上了`@EnableConfigurationProperties`注解。因此，除非你完全不使用自动配置(那怎么可能?)，否则就 无需显式地添加`@EnableConfigurationProperties`。
还有一点需要注意，Spring Boot的属性解析器非常智能，它会自动把驼峰规则的属性和使用
连字符或下划线的同名属性关联起来。换句话说，`amazon.associateId`这个属性和
`amazon.associate_id`以及`amazon.associate-id`都是等价的。用你习惯的命名规则就好。

### 2.3、使用Profile进行配置
当应用程序需要部署到不同的运行环境时，一些配置细节通常会有所不同。比如，数据库连 接的细节在开发环境下和测试环境下就会不一样，在生产环境下又不一样。Spring Framework从 Spring 3.1开始支持基于Profile的配置。Profile是一种条件化配置，基于运行时激活的Profile，会 使用或者忽略不同的Bean或配置类。
设置spring.profiles.active属性就能激活Profile，任意设置配置属性的方式都能用于 设置这个值。
Spring Boot支持为application.properties和application.yml里的属性配置
Profile。
为了演示区分Profile的属性，假设你希望针对生产环境和开发环境能有不同的日志配置。在
生产环境中，你只关心WARN或更高级别的日志项，想把日志写到日志文件里。在开发环境中， 你只想把日志输出到控制台，记录DEBUG或更高级别。
而你所要做的就是为每个环境分别创建配置。那要怎么做呢?这取决于你用的是属性文件配 置还是YAML配置。
#### 1.使用特定于Profile的属性文件
如果你正在使用application.properties，可以创建额外的属性文件，遵循application-{profile}. properties这种命名格式，这样就能提供特定于Profile的属性了。
在日志这个例子里，开发环境的配置可以放在名为application-development.properties的文件 里，配置包含日志级别和输出到控制台:
```
logging.level.root=DEBUG
```
对于生产环境，application-production.properties会将日志级别设置为WARN或更高级别，并将 日志写入日志文件:
```
logging.path=/var/logs/
logging.file=BookWorm.log
logging.level.root=WARN
```
与此同时，那些并不特定于哪个Profile或者保持默认值(以防万一有哪个特定于Profile的配 置不指定这个值)的属性，可以继续放在application.properties里:
```
amazon.associateId=habuma-20
logging.level.root=INFO
```
#### 2. 使用多Profile YAML文件进行配置
&emsp;&emsp;如果使用YAML来配置属性，则可以遵循与配置文件相同的命名规范，即创建application- {profile}.yml这样的YAML文件，并将与Profile无关的属性继续放在application.yml里。
&emsp;&emsp;但既然用了YAML，你就可以把所有Profile的配置属性都放在一个application.yml文件里。举例来说，我们可以像下面这样声明日志配置:
```yml
logging:
  level:
    root: INFO
---
spring:
  profiles: development
logging:
  level:
    root: DEBUG
---
spring:
  profiles: production
logging:
  path: /tmp/
  file: BookWorm.log
  level:
root: WARN
```
如你所见，这个application.yml文件分为三个部分，使用一组三个连字符(---)作为分隔符。 第二段和第三段分别为spring.profiles指定了一个值，这个值表示该部分配置应该应用在哪 个Profile里。第二段中定义的属性应用于开发环境，因为spring.profiles设置为 development。与之类似，最后一段的spring.profile设置为production，在production Profile被激活时生效。
另一方面，第一段并未指定spring.profiles，因此这里的属性对全部Profile都生效，或 者对那些未设置该属性的激活Profile生效。
除了自动配置和外置配置属性，Spring Boot还有其他简化常用开发任务的绝招:它自动配置了一个错误页面，在应用程序遇到错误时显示。

### 2.4、定制应用程序错误页面
错误总是会发生的，那些在生产环境里最健壮的应用程序偶尔也会遇到麻烦。虽然减小用户 遇到错误的概率很重要，但让应用程序展现一个好的错误页面也同样重要。
近年来，富有创意的错误页已经成为了一种艺术。如果你曾见到过GitHub.com的星球大战错 误页，或者是DropBox.com的Escher立方体错误页的话，你就能明白我在说什么了。
我不知道你在使用阅读列表应用程序时有没有碰到错误，如果有的话，你看到的页面应该和 图3-1里的很像。
Spring Boot默认提供这个“白标”(whitelabel)错误页，这是自动配置的一部分。虽然这比 Stacktrace页面要好一点，但和网上那些伟大的错误页艺术品却不可同日而语。为了让你的应用程序故障页变成大师级作品，你需要为应用程序创建一个自定义的错误页。
Spring Boot自动配置的默认错误处理器会查找名为error的视图，如果找不到就用默认的白标 错误视图，如图3-1所示。因此，最简单的方法就是创建一个自定义视图，让解析出的视图名为error。
这一点归根到底取决于错误视图解析时的视图解析器。
* 实现了Spring的View接口的Bean，其 ID为error(由Spring的BeanNameViewResolver所解析)。
* 如果配置了Thymeleaf，则有名为error.html的Thymeleaf模板。
* 如果配置了FreeMarker，则有名为error.ftl的FreeMarker模板。
* 如果配置了Velocity，则有名为error.vm的Velocity模板。
* 如果是用JSP视图，则有名为error.jsp的JSP模板。

默认情况 下，Spring Boot会为错误视图提供如下错误属性。
* timestamp:错误发生的时间。 
* status:HTTP状态码。
* error:错误原因。
* exception:异常的类名。
* message:异常消息(如果这个错误是由异常引起的)。
* errors:BindingResult异常里的各种错误(如果这个错误是由异常引起的)。
* trace:异常跟踪信息(如果这个错误是由异常引起的)。 
* path:错误发生时请求的URL路径。

其中某些属性，比如path，在向用户交待问题时还是很有用的。其他的，比如trace，用起来要保守一点，将其隐藏，或者用得聪明点，让错误页尽可能对用户友好。

## 3. 小结
Spring Boot消除了Spring应用程序中经常要用到的很多样板式配置。让Spring Boot处理全部配置，你可以仰仗它来配置那些适合你的应用程序的组件。当自动配置无法满足需求时，Spring Boot允许你覆盖并微调它提供的配置。
覆盖自动配置其实很简单，就是显式地编写那些没有Spring Boot时你要做的Spring配置。 Spring Boot的自动配置被设计为优先使用应用程序提供的配置，然后才轮到自己的自动配置。
即使自动配置合适，你仍然需要调整一些细节。Spring Boot会开启多个属性解析器，让你通 过环境变量、属性文件、YAML文件等多种方式来设置属性，以此微调配置。这套基于属性的配 置模型也能用于应用程序自己定义的组件，可以从外部配置源加载属性并注入到Bean里。
Spring Boot还自动配置了一个简单的白标错误页，虽然它比异常跟踪信息友好一点，但在艺 术性方面还有很大的提升空间。幸运的是，Spring Boot提供了好几种选项来自定义或完全替换这 个白标错误页，以满足应用程序的特定风格。