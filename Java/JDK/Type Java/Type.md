# Type —— JAVA 类型
[部分摘自博客](https://blog.csdn.net/a327369238/article/details/52621043)
 ```java
 package java.lang.reflect;

/**
 * Type is the common superinterface for all types in the Java
 * programming language. These include raw types, parameterized types,
 * array types, type variables and primitive types.
 *
 * @since 1.5
 */
public interface Type {
    /**
     * Returns a string describing this type, including information
     * about any type parameters.
     *
     * @implSpec The default implementation calls {@code toString}.
     *
     * @return a string describing this type
     * @since 1.8
     */
    default String getTypeName() {
        return toString();
    }
}
 ```
Type 是一个 Java 编程语言里 **所有类型** 的公共父接口，这些类型包含了 原始类型(raw types，对应class)、 参数化类型(parameterized types)、数组类型(array types)、变量类型(type variables)、原始基本类型,对应class(primitive types)

1. raw types 代表的不仅仅是指我们平常使用的或自定义的 class 同时还包括了数组、接口、注解、枚举 等类型
2. array types 应该指的是 parameterized types 和 type variables 类型的数组，不是我们常用的 原始类型数组
3. List<T ? entends>[]：这里的List就是ParameterizedType，T就是TypeVariable，T ? entends就是WildcardType（注意，WildcardType不是Java类型，而是一个表达式），整个List<T ? entends>[]就是GenericArrayType。

Type 一共有4个子接口，分别是 ParameterizedType 、TypeVariable 、GenericArrayType、 WildcardType
## Type子接口解析
### 1.ParameterizedType
ParameterizedType 代表的 parameterized types 参数化类型，类似于 Collection<String>; 详情见 [ParameterizedType](ParameterizedType.md)

### 2. TypeVariable
TypeVariable 如参数化类型中的E、K等类型变量，表示泛指任何类，如果加上extends/super限定，则就会有相应的上限、下限。

### 3.GenericArrayType
GenericArrayType 范型数组，表示上面两种的数组类型，即如： A<T>[], T[][]类型

#### 4.WildcardType
WildcardType 通配符类型或泛型表达式，它虽然是Type的一个子接口，但并不是Java类型中的一种，表示的仅仅是类似 ? extends T、? super K这样的通配符表达式。


