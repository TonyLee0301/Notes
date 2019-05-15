#1. 考虑使用静态方法而不是构造器
*[traditional]: ajd. 传统的；惯例的
*[obtain]: vt. vi. 获得；流行
*[Note]:vt. 注意；记录；注解
*[Design Patterns]: 设计模式
*[equivalent]: adj. 等价的，相等的 n. 等价物，相等物；同意义的
*[in addition to]: 除此之外
*[addition]: n.添加、增加
*[instead]: adv. 代替；反而；相反
*[advantage]:n. 优势；特点；有利；v. 促进
*[advantages]:n. 优势；特点；有利；[经] 利益（advantage的复数）v. 促进（advantage的三单形式）；有利于
*[disadvantages]:n. 劣势；不利条件；损害（disadvantage的复数）v. 使处于不利地位；使受损（disadvantage的三单形式）
*[themselves]:pron. 他们自己；他们亲自
*[probably]:adv. 大概 或许
*[prime]: 素数 adj. 主要的；最好的；基本的 adv. 极好地   n. 初期；青年；精华；全盛时期 vt. 使准备好；填装 vi. 作准备
*[restriction]: n.限制、约束
*[previous]: adj.先前的
*[paragraph]: n.段落/短语
*[multiple]: n.倍数的,adj.多个部分组成/多处
*[immutable]: adj.一成不变/不可变
*[preconstructed]: 来自pre （预）- +construct （构建）+-ed adj.预构建，wiki可查
*[dispense]: vt.分配/分发/施与/给予/执行/配发
*[repeatedly]: adv.反复
*[avoid]: v.避免、防止
*[illustrates]: v.说明、绘图、演示
*[often]: adv.经常
*[especially]: adv. 特别、尤其
*[expensive]: adj.昂贵、高价
*[repeated]: adj.反复、再三 v.重复
*[maintain]: vt.维持、保持
*[strict]: adj.严格、严谨
*[guarantee]: v. 保证、担保
*[noninstantiable]: non-instantiable 不可实现
*[fashion]: n.时尚、时装；方式、方法 vt.使用，改变
*[compact]: vt.紧凑 adj.紧凑的、简洁的 n.合同契约
*[natural]: ajd.自然的、物质的 n.自然的事
*[flexibility]: n. 灵活性；适应性
*[Prior]: adv. 在前，居先 adj. 优先的；在先的，在前的
*[convention]: n.惯例、公约
*[convenience]: n.方便；便利；厕所
*[companion]: n.同伴、伴侣 vt.陪伴
*[utility]: n.实用，效用 ajd.实用的，通用的
*[unmodifiable]: adj.无法改变的
*[via]: adv. 通过、经由， prep. 渠道，通过
*[reduced]: adj.减少的、【数】简化的；缩减的
*[bulk]: n.体积，容量；大多数，大部分，大块;vt.使扩大，使形成大量的。
*[conceptual]: adj.概念上的
*[concepts]: n. 概念，观念；思想（concept复数形式）
*[precisely]: adv.精确的；恰恰


The *traditional*` ajd. 传统的；惯例的 ` way for a class to allow a client to *obtain* ` vt. vi. 获得；流行 ` an instance is to provide a public constructor. There is another technique that should be a part of every programmer’s toolkit. A class can provide a public static factory method, which is simply a static method that returns an instance of the class. Here’s a simple example from Boolean(the boxed primitive class for boolean). This method translates a boolean primitive value into a Boolean object reference:
```java{.line-numbers}
public static Boolean valueOf(boolean b) { 
    return b ? Boolean.TRUE : Boolean.FALSE;
}
```
*Note* `vt. 注意；记录；注解` that a static factory method is not the same as the Factory Method pattern from *Design Patterns*`设计模式`[Gamma95]. The static factory method described in this item has no direct *equivalent*` adj. 等价的，相等的 n. 等价物，相等物；同意义的` in Design Patterns.

A class can provide its clients with static factory methods instead of, or *in addition to* `除此之外`, public constructors. Providing a static factory method *instead* `adv. 代替；反而；相反` of a public constructor has both *advantages* `n. 优势；特点；有利；[经] 利益（advantage的复数）, v. 促进（advantage的三单形式）；有利于` and *disadvantages* `advantages 反义词`.

One advantage of static factory methods is that, unlike constructors, they have names. If the parameters to a constructor do not, in and of *themselves* `pron. 他们自己；他们亲自`, describe the object being returned, a static factory with a well-chosen name is easier to use and the resulting client code easier to read. For example, the constructor BigInteger(int, int, Random), which returns a BigInteger that is *probably* `adv. 大概 或许` *prime* `指素数`, would have been better expressed as a static factory method named BigInteger.probablePrime. (This method was added in Java4.)

A class can have only a single constructor with a given signature. Programmers have been known to get around this *restriction* `n. 限制、约束` by providing two constructors whose parameter lists differ only in the order of their parameter types. This is a really bad idea. The user of such an API will never be able to remember which constructor is which and will end up calling the wrong one by mistake. People reading code that uses these constructors will not know what the code does without referring to the class documentation.

Because they have names, static factory methods don’t share the restriction discussed in the *previous* `adj.先前的` *paragraph* `n.段落/短文`. In cases where a class seems to require *multiple* `n.倍数，adj.多个` constructors with the same signature, replace the constructors with static factory methods and carefully chosen names to highlight their differences.

A second advantage of static factory methods is that, unlike constructors, they are not required to create a new object each time they’re invoked. This allows *immutable* `adj.一成不变/不可变` classes (Item 17) to use *preconstructed* `来自pre- +construct+-ed adj.预构建` instances, or to cache instances as they’re constructed, and *dispense* `vt.分配/分发/施与/给予/执行/配发` them *repeatedly* `adv.反复` to *avoid* `v.避免、防止` creating unnecessary duplicate objects.

The Boolean.valueOf(boolean) method *illustrates* `v.说明、绘图、演示` this technique: it never creates an object. This technique is similar to the Flyweight pattern [Gamma95]. It can greatly improve performance if equivalent objects are requested *often* `adv.经常`, *especially* `adv. 特别、尤其` if they are *expensive* `adj.昂贵、高价` to create.

The ability of static factory methods to return the same object from *repeated* `adj.反复，再三 v.重复` invocations allows classes to *maintain*`vt.维持、保持` *strict* `adj.严格、严谨` control over what instances exist at any time. Classes that do this are said to be instance-controlled.There are several reasons to write instance-controlled classes. Instance control allows a class to *guarantee* `v.保证、担保` that it is a singleton (Item 3) or *noninstantiable* `non-instantiable 不可实现` (Item 4). Also, it allows an immutable value class (Item 17) to make the guarantee that no two equal instances exist: a.equals(b) if and only if a == b. This is the basis of the Flyweight pattern [Gamma95]. Enum types (Item 34) provide this guarantee.

__A third advantage of static factory methods is that, unlike constructors, they can return an object of any subtype of their return type.__ This gives you great *flexibility* ` n.灵活性` in choosing the class of the returned object.

One application of this flexibility is that an API can return objects without making their classes public. Hiding implementation classes in this fashion leads to a very *compact* `vt.紧凑 adj.紧凑的、简洁的 n.合同契约` API. This technique lends itself to interface-based frameworks(Item 20), where interfaces provide natural return types for static factory methods.

*Prior* `adv. 在前，居先 adj. 优先的；在先的，在前的` to Java 8, interfaces couldn’t have static methods. By *convention* `n.惯例、公约`, static factory methods for an interface named Type were put in a noninstantiable *companion* `n.同伴、伴侣 vt.陪伴` class(Item 4) named Types. For example, the Java Collections Framework has forty-five *utility* `n.实用，效用 ajd.实用的，通用的` implementations of its interfaces, providing *unmodifiable* `adj.无法改变的` collections, synchronized collections, and the like. Nearly all of these implementations are exported *via* `adv. 通过、经由， prep. 渠道，通过` static factory methods in one noninstantiable class (java.util.Collections). The classes of the returned objects are all nonpublic.

The Collections Framework API is much smaller than it would have been had it exported forty-five separate public classes, one for each *convenience* `n.方便；便利；厕所` implementation. It is not just the bulk of the API that is *reduced* `adj.减少的、【数】简化的；缩减的` but the *conceptual* `adj.概念上的` weight:the number and difficulty of the *concepts* `n. 概念，观念；思想（concept复数形式）` that programmers must master in order to use the API. The programmer knows that the returned object has *precisely* `adv.精确的；恰恰` the API specified by its interface, so there is no need to read additional class documentation for the implementation class. Furthermore, using such a static factory method requires the client to refer to the returned object by interface rather than implementation class, which is generally good practice (Item 64).
As of Java 8, the restriction that interfaces cannot contain static methods was eliminated, so there is typically little reason to provide a noninstantiable companion class for an interface. Many public static members that would have been at home in such a class should instead be put in the interface itself. Note, however, that it may still be necessary to put the bulk of the implementation code behind these static methods in a separate package-private class. This is because Java 8 requires all static members of an interface to be public. Java 9 allows private static methods, but static fields and static member classes are still required to be public.

