
<!-- @import "[TOC]" {cmd="toc" depthFrom=1 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->

- [Bean加载](#bean加载)
  - [1 FactoryBean 的使用](#1-factorybean-的使用)
  - [2 缓存总获取单例](#2-缓存总获取单例)
  - [3 从bean的实例中获取对象](#3-从bean的实例中获取对象)
  - [4 获取单例](#4-获取单例)
  - [5 准备创建bean](#5-准备创建bean)
    - [5.1 处理override属性](#51-处理override属性)
    - [5.2 实例化的前置处理](#52-实例化的前置处理)
  - [6 循环依赖](#6-循环依赖)
  - [7 创建 bean](#7-创建-bean)
      - [7.1 创建bean的实例](#71-创建bean的实例)
    - [7.1.1 ConstructorResolver#autowiredConstructor](#711-constructorresolverautowiredconstructor)
    - [7.1.2 AbstractAutowireCapableBeanFactory#instantiateBean](#712-abstractautowirecapablebeanfactoryinstantiatebean)
    - [7.1.3 实例化策略](#713-实例化策略)
  - [7.2 记录创建bean的ObjectFactory](#72-记录创建bean的objectfactory)
  - [7.3 属性注入](#73-属性注入)
    - [7.3.1 autowireByName](#731-autowirebyname)
    - [7.3.2 autowireByType](#732-autowirebytype)
    - [7.3.3 applyPropertyValues](#733-applypropertyvalues)
  - [7.4 初始化 bean](#74-初始化-bean)
    - [7.4.1 激活 Aware 方法](#741-激活-aware-方法)

<!-- /code_chunk_output -->

# Bean加载
&emsp;&emsp;根据之前我们的分析，我们了解到了Spring对XML配置文件的解析，生成对应的BeanDefinition对象，那么具体Bean又是在什么时候加载的呢？对于bean的加载，spring的调用方式为：
> AbstractBeanFactory#getBean
 ```java
    TestBean emma = (TestBean) xbf.getBean("emma")

    // AbstractBeanFactory.getBean
    @Override
	public Object getBean(String name) throws BeansException {
		return doGetBean(name, null, null, false);
	}
    
    protected <T> T doGetBean(final String name, @Nullable final Class<T> requiredType,
			@Nullable final Object[] args, boolean typeCheckOnly) throws BeansException {
		//转换beanName
		final String beanName = transformedBeanName(name);
		Object bean;

		// Eagerly check singleton cache for manually registered singletons.
		//手动的检查单例缓存中是否存在注册的单例，该 sharedInstance 可能是个 ObjectFactory 也可能是 一个具体的实例
		/**
		 * 检查缓存中或者实例工厂中是否有对应的实例
		 * 为什么首先会使用这段代码呢，
		 * 因为在创建单例bean的时候会存在依赖注入，而在创建的时候为了避免循环依赖，
		 * Spring 创建 bean 的原则是不等bean创建完成就不会将创建bean的ObjectFactory提早曝光也就是将 ObjectFactory 加入到缓存中，一旦下一个bean创建时候需要依赖上bean的时候直接使用ObjectFactory
		 */
		Object sharedInstance = getSingleton(beanName);
		if (sharedInstance != null && args == null) {
			if (logger.isTraceEnabled()) {
				if (isSingletonCurrentlyInCreation(beanName)) {
					logger.trace("Returning eagerly cached instance of singleton bean '" + beanName +
							"' that is not fully initialized yet - a consequence of a circular reference");
				}
				else {
					logger.trace("Returning cached instance of singleton bean '" + beanName + "'");
				}
			}
			//getObjectForBeanInstance 从命名可以看出，获取一个object用于实例bean
			//在一定的情况下，有时候返回的并不是实例本身而是通过指定方法返回的实例
			bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
		}

		else {
			// Fail if we're already creating this bean instance:
			// We're assumably within a circular reference.
			//只有在单例情况才会尝试解决循环依赖，原型模式情况下： A 属性中有B的属性，B属性中有A的属性，
			// 那么在依赖注入的时候，就会产生当A还未创建完成的时候因为对B的创建再次返回创建A，造成循环依赖
			// beforePrototypeCreation(beanName)
			if (isPrototypeCurrentlyInCreation(beanName)) {
				throw new BeanCurrentlyInCreationException(beanName);
			}

			// Check if bean definition exists in this factory.
			//判断当前bean definition 是否在当前的 bean factory 中存在
			BeanFactory parentBeanFactory = getParentBeanFactory();
			//当父 factory 不为空，且当前不存在bean definition时，使用父factory去加载bean
			if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
				// Not found -> check parent.
				String nameToLookup = originalBeanName(name);
				if (parentBeanFactory instanceof AbstractBeanFactory) {
					return ((AbstractBeanFactory) parentBeanFactory).doGetBean(
							nameToLookup, requiredType, args, typeCheckOnly);
				}
				else if (args != null) {
					// Delegation to parent with explicit args.
					return (T) parentBeanFactory.getBean(nameToLookup, args);
				}
				else if (requiredType != null) {
					// No args -> delegate to standard getBean method.
					return parentBeanFactory.getBean(nameToLookup, requiredType);
				}
				else {
					return (T) parentBeanFactory.getBean(nameToLookup);
				}
			}

			//如果不仅仅是做类型检查这是创建bean。这里做记录
			if (!typeCheckOnly) {
				markBeanAsCreated(beanName);
			}

			try {
				//将解析xml中创建的 GernericBeanDefinition 转换为 RootBeanDefinition，并且如果指定的beanName是子Bean的话同时会合并父类的相关属性
				final RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
				checkMergedBeanDefinition(mbd, beanName, args);

				// Guarantee initialization of beans that the current bean depends on.
				// 获取bean的依赖，在初始化bean之前，先初始化其依赖的bean
				String[] dependsOn = mbd.getDependsOn();
				if (dependsOn != null) {
					for (String dep : dependsOn) {
						if (isDependent(beanName, dep)) {
							throw new BeanCreationException(mbd.getResourceDescription(), beanName,
									"Circular depends-on relationship between '" + beanName + "' and '" + dep + "'");
						}
						//注册依赖关系
						registerDependentBean(dep, beanName);
						try {
							//加载bean
							getBean(dep);
						}
						catch (NoSuchBeanDefinitionException ex) {
							throw new BeanCreationException(mbd.getResourceDescription(), beanName,
									"'" + beanName + "' depends on missing bean '" + dep + "'", ex);
						}
					}
				}

				// Create bean instance.
				//创建bean
				if (mbd.isSingleton()) {
					//初始化单例bean，创建一个 ObjectFactory 用于初始化bean
					sharedInstance = getSingleton(beanName, () -> {
						try {
							return createBean(beanName, mbd, args);
						}
						catch (BeansException ex) {
							// Explicitly remove instance from singleton cache: It might have been put there
							// eagerly by the creation process, to allow for circular reference resolution.
							// Also remove any beans that received a temporary reference to the bean.
							destroySingleton(beanName);
							throw ex;
						}
					});
					bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
				}
				// 原型模式下的创建bean
				else if (mbd.isPrototype()) {
					// It's a prototype -> create a new instance.
					Object prototypeInstance = null;
					try {
						beforePrototypeCreation(beanName);
						prototypeInstance = createBean(beanName, mbd, args);
					}
					finally {
						afterPrototypeCreation(beanName);
					}
					bean = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
				}

				else {
					String scopeName = mbd.getScope();
					final Scope scope = this.scopes.get(scopeName);
					if (scope == null) {
						throw new IllegalStateException("No Scope registered for scope name '" + scopeName + "'");
					}
					try {
						Object scopedInstance = scope.get(beanName, () -> {
							beforePrototypeCreation(beanName);
							try {
								return createBean(beanName, mbd, args);
							}
							finally {
								afterPrototypeCreation(beanName);
							}
						});
						bean = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
					}
					catch (IllegalStateException ex) {
						throw new BeanCreationException(beanName,
								"Scope '" + scopeName + "' is not active for the current thread; consider " +
								"defining a scoped proxy for this bean if you intend to refer to it from a singleton",
								ex);
					}
				}
			}
			catch (BeansException ex) {
				cleanupAfterBeanCreationFailure(beanName);
				throw ex;
			}
		}

		// Check if required type matches the type of the actual bean instance.
		// 检查需要的类型是否符合bean的实际类型
		if (requiredType != null && !requiredType.isInstance(bean)) {
			try {
				T convertedBean = getTypeConverter().convertIfNecessary(bean, requiredType);
				if (convertedBean == null) {
					throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
				}
				return convertedBean;
			}
			catch (TypeMismatchException ex) {
				if (logger.isTraceEnabled()) {
					logger.trace("Failed to convert bean '" + name + "' to required type '" +
							ClassUtils.getQualifiedName(requiredType) + "'", ex);
				}
				throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
			}
		}
		return (T) bean;
	}

 ```
&emsp;&emsp;上面的代码大致的加载过程步骤大致如下：
1. beanName的转换
    为什么会存在beanName转换，传入的参数 name 不就是beanName么。其实不然，这里传入的参数有可能是别名，有可能是FactoryBean，所以需要进行一系列的解析，这些解析的内容如下内容。
    * 去除FactoryBean的修饰符，例如:name=&aa,beanName = aa;
    * 去取指定的alias所表示的beanName。前面我们在解析xml的时候，已经将相关alias注册到了AliasRegistry中，因此可以从中获取到最终的bean。
2. 尝试从缓存中加载
    单例的在Spring的同一个容器中只会创建一次，后续获取bean，直接从缓存中获取。当然这里是尝试加载。如果在singletonObjects中获取不到，则尝试从singletonFactories中加载。因为在创建单例bean的时候为了避免循环依赖，在spring中创建bean的原则是不等bean创建完成就会创建bean的ObjectFactory提前曝光在earlySingletonObjects缓存中，一旦下一个bean创建时候需要依赖上一个bean则直接使用ObjectFactory。
3. bean的实例化
    如果从缓存中获取到了bean的原始状态，这需要对bean进行实例话。这里有必要强调一下，珲春中记录的只是最原始的bean状态，并不是一定是我们最终想要的bean。如何解释？
    我们需要对工厂bean进行处理，那么这里得到的其实是工厂bean的初始化状态，但是我们真正需要的是工厂bean进行处理，那么这里得到的是工厂bean的初始化状态，但是我们真正需要的是工厂bean中定义的factory-method方法中返回的bean，而getObjectForBeanInstance就是完成这个工作的。
4. 原型模式的依赖检查
    只有在单例的情况下会尝试解决循环依赖，如果存在A中有B的属性，B中有A的属性，那么当依赖注入的时候，原型模式，就会不停的创建新的A，B两个bean造成循环依赖，isPrototypeCurrentlyInCreation就是用来处理这种情况。
5. 检测parentBeanFactory
    从代码上看，如果缓存没有数据的化，直接转到父工厂上去加载。这里有个判断条件，`parentBeanFactory != null && !containsBeanDefinition(beanName)` ，也就是所需要保证 parentBeanFactory 不为空，且当前 BeanFactory 中没有该beanName的注册，才会取父工厂取加载，然后递归调用getBean方法。
6. 将 GenericBeanDefinition 转换为 RootBeanDefinition。
    XML读取到的ben信息都是储存在 GenericBeanDefinition 中，但是所有的 Bean 后续处理 都是针对 RootBeanDefinition ，所以这里需要进行一个转换，转换的时候同时如果弗雷bean不为空的化，则会一并合并父类的属性。
7. 寻找bean的依赖
    在bean的初始化过程中可能会用到某些属性，而某些属性很可能是动态配置的，并且配置成依赖于其他的bean，那么这个时候就有必要先加载依赖的bean，所以，在Spring的加载顺序中，在初始化某一个bean的时候首先会初始化这个bean所对应的依赖。
8. 针对不通的scope进行bean的创建
    spring 中存在着不同的 scope，其中默认的是signleton，但是还有一些其他配置诸如 prototpe 、 request 之类的。在这个步骤中，Spring 会根据不通的配置进行不通的初始化策略。
9. 类型转换
    程序到这里返回的bean后已经基本结束了，通常对该方法的调用参数requeiredType是空的，但是可能会存在这种情况，返回的bean其实是一个String，但是requiredType却传入了Integer类型，那么这时候本步骤就会起作用了，它的功能将返回的bean转换为requiredType所指定的类型。当然String转换为Integer是最简单的一种转换，在Spring中提供了各种个样的转换器，用户也可以自己扩展转换器来满足需求。

## 1 FactoryBean 的使用
&emsp;&emsp;一般情况下，Spring通过反射机制利用 bean 的 class 属性指定实现类来实例化bean。 在某些情况下，实例化bean过程比较复杂，如果按照传统的方式，则需要在<bean>中提供大量的配置信息，配置方式的灵活性是受限的，这时采用编码的方式可能会得到一个简单的方案。 Spring 为此提供了一个 org.springframework.beans.factory.BeanFactory 的工厂类接口，用户可以通过实现该接口定制实例化bean的逻辑。
&emsp;&emsp;FactoryBean接口对于Spring框架来说占有重要的地位，Spring自身就提供了70多个FactoryBean的实现。它们隐藏了一些复杂bean的细节，给上层应用带来了便利。
 ```java
 public interface FactoryBean<T> {
    String OBJECT_TYPE_ATTRIBUTE = "factoryBeanObjectType";
    @Nullable
	T getObject() throws Exception;
    @Nullable
	Class<?> getObjectType();
    default boolean isSingleton() {
		return true;
	}
}
 ```

## 2 缓存总获取单例
&emsp;&emsp;了解bean的加载，提到过，单例在Spring的同一个容器总只会被创建一次，后续再获取bean直接从单例缓存总获取，当然这里也只是尝试加载，首先尝试从缓存总加载，然后再尝试从singletonFactories中加载。因为在创建单例bean的时候会存在依赖注入的情况，而在创建依赖的时候为了避免循环依赖,Spring创建bean的原则是不等bean创建完成就会将创建bean的ObjectFactory提前曝光加入到缓存中，一旦下一个bean创建时需要依赖上个bean，这直接使用ObjectFactory。
> DefaultSingletonBeanRegistry#getSingleton
 ```java
 	@Override
	@Nullable
	public Object getSingleton(String beanName) {
		//参数true允许早期依赖
		return getSingleton(beanName, true);
	}

	@Nullable
	protected Object getSingleton(String beanName, boolean allowEarlyReference) {
		//检查单例缓存中是否存在实例
		Object singletonObject = this.singletonObjects.get(beanName);
		//实例为空，并且该beanName正在被创建
		if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
			//锁单例缓存，再进行处理
			synchronized (this.singletonObjects) {
				//判断该beanName是否有在提前曝光的缓存中
				singletonObject = this.earlySingletonObjects.get(beanName);
				//如果为空，并且允许提前曝光，因为单例模式下，创建bean的时候如果存在依赖，会先创建依赖的bean，因此需要提前将将该beanName的相关信息暴露出来
				if (singletonObject == null && allowEarlyReference) {
					//根据获取beanName的ObjectFactory
					ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
					if (singletonFactory != null) {
						//通过singletonFactory创建实例，并存放到 earlySingletonObjects 中并从 singletonFactories 移除
						singletonObject = singletonFactory.getObject();
						this.earlySingletonObjects.put(beanName, singletonObject);
						this.singletonFactories.remove(beanName);
					}
				}
			}
		}
		return singletonObject;
	}
 ```
&emsp;&emsp;可以看到这里有好几个缓存bean的map，简单解释下：
* singletonObjects 用于保存 BeanName 和创建 bean 实例之间的关系， bean name -> bean instance。
* singletonFactories 用于保存 BeanName 和 创建 bean 工厂时间的关系, bean name -> ObjectFactory。
* earlySingletonObjects 也是保存 BeanName 和 创建 bean 实例之间的关系，与 singletonObjects 的不同之处在于，当一个 bean 被放到这里后，那么当bean还在创建过程中，就可以通过 getBean 方法获取到了，其目的是用来检测循环引用。
* registeredSingletons 用来保存所有已经注册的bean

## 3 从bean的实例中获取对象
&emsp;&emsp;在getBean方法中，getObjectForBeanInstance 是一个高频率使用的方法，无论是从缓存中获得bean，还是根据不同的 scope 策略加载bean。总之，我们得到bean的实例后要做的第一步就是调用这个方法来检测一下正确性，其实就用于检测当前bean是否是FactoryBean类型的bean，如果是，那么需要调用该bean对应的FactoryBean实例中的getObject()作为返回值。上面我们提到的 FactoryBean 的用途了，而 getObjectForBeanInstance 方法就是在加载过程中实现这个用途的工作。
> AbstractBeanFactory#getObjectForBeanInstance
 ```java
 protected Object getObjectForBeanInstance(
			Object beanInstance, String name, String beanName, @Nullable RootBeanDefinition mbd) {

		// Don't let calling code try to dereference the factory if the bean isn't a factory.
		//判断name是否是工厂相关(以&前缀开头的)
		if (BeanFactoryUtils.isFactoryDereference(name)) {
			if (beanInstance instanceof NullBean) {
				return beanInstance;
			}
			if (!(beanInstance instanceof FactoryBean)) {
				throw new BeanIsNotAFactoryException(beanName, beanInstance.getClass());
			}
			if (mbd != null) {
				mbd.isFactoryBean = true;
			}
			return beanInstance;
		}

		// Now we have the bean instance, which may be a normal bean or a FactoryBean.
		// If it's a FactoryBean, we use it to create a bean instance, unless the
		// caller actually wants a reference to the factory.
		//如果不是 factoryBean 则直接返回实例
		if (!(beanInstance instanceof FactoryBean)) {
			return beanInstance;
		}

		Object object = null;
		if (mbd != null) {
			mbd.isFactoryBean = true;
		}
		else {
			//从缓存中获取factoryBean
			object = getCachedObjectForFactoryBean(beanName);
		}
		if (object == null) {
			// Return bean instance from factory.
			// 通过 factory 返回一个 bean 实例
			FactoryBean<?> factory = (FactoryBean<?>) beanInstance;
			// Caches object obtained from FactoryBean if it is a singleton.
			//当 RootBeanDefinition 为空，并且 beanDefinitionMap 中包含这个 beanName
			if (mbd == null && containsBeanDefinition(beanName)) {
				mbd = getMergedLocalBeanDefinition(beanName);
			}
			boolean synthetic = (mbd != null && mbd.isSynthetic());
			object = getObjectFromFactoryBean(factory, beanName, !synthetic);
		}
		return object;
	}
 ```
&emsp;&emsp;getObjectForBeanInstance 主要做了些功能性的判断和准备，真正的核心代码委托给了 getObjectFromFactoryBean，getObjectForBeanInstance主要的工作如下：
1. 对FactoryBean正确性的验证。
2. 对非Factory不做任何处理。
3. 对 beanDefinition 进行装欢。
4. 将从Factory中解析bean的工作委托给 getObjectFromFactoryBean 。

 ```java
 protected Object getObjectFromFactoryBean(FactoryBean<?> factory, String beanName, boolean shouldPostProcess) {
		//如果是单例，并且在 存在该beanBean的定义
		if (factory.isSingleton() && containsSingleton(beanName)) {
			synchronized (getSingletonMutex()) {
				//尝试从factoryBeanCache缓存中加载
				Object object = this.factoryBeanObjectCache.get(beanName);
				if (object == null) {
					//真正处理bean的方法
					object = doGetObjectFromFactoryBean(factory, beanName);
					// Only post-process and store if not put there already during getObject() call above
					// (e.g. because of circular reference processing triggered by custom getBean calls)
					//这里再次尝试从缓存中加载bean
					Object alreadyThere = this.factoryBeanObjectCache.get(beanName);
					//如果已经存在，则返回缓存中的object
					if (alreadyThere != null) {
						object = alreadyThere;
					}
					else {
						//如果不存在，这进行bean初始化的后置处理
						if (shouldPostProcess) {
							if (isSingletonCurrentlyInCreation(beanName)) {
								// Temporarily return non-post-processed object, not storing it yet..
								return object;
							}
							//验证当前beanName是否在加载中
							beforeSingletonCreation(beanName);
							try {
								//
								object = postProcessObjectFromFactoryBean(object, beanName);
							}
							catch (Throwable ex) {
								throw new BeanCreationException(beanName,
										"Post-processing of FactoryBean's singleton object failed", ex);
							}
							finally {
								//清楚相关缓存
								afterSingletonCreation(beanName);
							}
						}
						if (containsSingleton(beanName)) {
							//将bean实例存放到缓存中
							this.factoryBeanObjectCache.put(beanName, object);
						}
					}
				}
				return object;
			}
		}
		else {
			Object object = doGetObjectFromFactoryBean(factory, beanName);
			if (shouldPostProcess) {
				try {
					object = postProcessObjectFromFactoryBean(object, beanName);
				}
				catch (Throwable ex) {
					throw new BeanCreationException(beanName, "Post-processing of FactoryBean's object failed", ex);
				}
			}
			return object;
		}
	}
 ```
&emsp;&emsp;这个方法里也没有具体的实例化bean，而是又委托给了 doGetObjectFromFactoryBean 。而在这个方法主要就做了两件事：如果返回的bean，是单例则，保证全局唯一，同时针对 bean 的初始化后置处理，并将实例话的bean存放到缓存中，以便与复用。
bean初始化的后置处理，这里不做过多介绍。
> AbstractAutowireCapableBeanFactory#postProcessObjectFromFactoryBean
 ```java
 	@Override
	protected Object postProcessObjectFromFactoryBean(Object object, String beanName) {
		return applyBeanPostProcessorsAfterInitialization(object, beanName);
	}
	@Override
	public Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName)
			throws BeansException {

		Object result = existingBean;
		for (BeanPostProcessor processor : getBeanPostProcessors()) {
			Object current = processor.postProcessAfterInitialization(result, beanName);
			if (current == null) {
				return result;
			}
			result = current;
		}
		return result;
	}
 ```
&emsp;&emsp;在 doGetObjectFromFactoryBean 方法中我们终于找到了相关的方法。
 ```java
 private Object doGetObjectFromFactoryBean(final FactoryBean<?> factory, final String beanName)
			throws BeanCreationException {

		Object object;
		try {
			if (System.getSecurityManager() != null) {
				AccessControlContext acc = getAccessControlContext();
				try {
					object = AccessController.doPrivileged((PrivilegedExceptionAction<Object>) factory::getObject, acc);
				}
				catch (PrivilegedActionException pae) {
					throw pae.getException();
				}
			}
			else {
				object = factory.getObject();
			}
		}
		catch (FactoryBeanNotInitializedException ex) {
			throw new BeanCurrentlyInCreationException(beanName, ex.toString());
		}
		catch (Throwable ex) {
			throw new BeanCreationException(beanName, "FactoryBean threw exception on object creation", ex);
		}

		// Do not accept a null value for a FactoryBean that's not fully
		// initialized yet: Many FactoryBeans just return null then.
		if (object == null) {
			if (isSingletonCurrentlyInCreation(beanName)) {
				throw new BeanCurrentlyInCreationException(
						beanName, "FactoryBean which is currently in creation returned null from getObject");
			}
			object = new NullBean();
		}
		return object;
	}
 ```

## 4 获取单例
&emsp;&emsp;之前我们讲解的都是在缓存中加载到beanName的情况，如果缓存中不存在已加载的单例bean，就需要从头加载bean的过程了，而Spring中使用getSingleton的虫灾方法实现bean的加载工程。
> DefaultSingletonBeanRegistry#getSingleton
 ```java
 public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
		Assert.notNull(beanName, "Bean name must not be null");
		//单例缓存的全局变量同步
		synchronized (this.singletonObjects) {
			//尝试从单例加载
			Object singletonObject = this.singletonObjects.get(beanName);
			//为空，开始准备初始化bean的初始化
			if (singletonObject == null) {
				if (this.singletonsCurrentlyInDestruction) {
					throw new BeanCreationNotAllowedException(beanName,
							"Singleton bean creation not allowed while singletons of this factory are in destruction " +
							"(Do not request a bean from a BeanFactory in a destroy method implementation!)");
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Creating shared instance of singleton bean '" + beanName + "'");
				}
				//缓存bean加载的缓存，循环依赖检查
				beforeSingletonCreation(beanName);
				boolean newSingleton = false;
				boolean recordSuppressedExceptions = (this.suppressedExceptions == null);
				if (recordSuppressedExceptions) {
					this.suppressedExceptions = new LinkedHashSet<>();
				}
				try {
					//初始化bean
					singletonObject = singletonFactory.getObject();
					newSingleton = true;
				}
				catch (IllegalStateException ex) {
					// Has the singleton object implicitly appeared in the meantime ->
					// if yes, proceed with it since the exception indicates that state.
					singletonObject = this.singletonObjects.get(beanName);
					if (singletonObject == null) {
						throw ex;
					}
				}
				catch (BeanCreationException ex) {
					if (recordSuppressedExceptions) {
						for (Exception suppressedException : this.suppressedExceptions) {
							ex.addRelatedCause(suppressedException);
						}
					}
					throw ex;
				}
				finally {
					if (recordSuppressedExceptions) {
						this.suppressedExceptions = null;
					}
					//清除bean加载的缓存,循环依赖
					afterSingletonCreation(beanName);
				}
				if (newSingleton) {
					//加入缓存
					addSingleton(beanName, singletonObject);
				}
			}
			return singletonObject;
		}
	}
 ```
&emsp;&emsp;上面的代码其实也没有具体的获取单例bean的方法，而是通过ObjectFactory的getObject方法回调实现的。该方法，更多的是做相关准备和检查：
1. 检查缓存是否已经加载过。
2. 若没有加载，记录beanName的正在加载状态。
3. 加载单例前记录加载状态。beforeSingletonCreation,这样可以对循环依赖进行检测。
 ```java
 	protected void beforeSingletonCreation(String beanName) {
		if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.add(beanName)) {
			throw new BeanCurrentlyInCreationException(beanName);
		}
	}
 ```
4. 通过传入的ObjectFactory#getObject方法实例化bean。
5. 加载单例后的处理方法调用。与第3步相似，移除对该bean的加载状态的记录。
6. 将结果记录至缓存并删除加载bean过程中说记录的各种辅助状态。
 ```java
 	protected void addSingleton(String beanName, Object singletonObject) {
		synchronized (this.singletonObjects) {
			this.singletonObjects.put(beanName, singletonObject);
			this.singletonFactories.remove(beanName);
			this.earlySingletonObjects.remove(beanName);
			this.registeredSingletons.add(beanName);
		}
	}
 ```
7. 返回处理bean实例。
&emsp;&emsp;我们从外部已经了解了加载bean的逻辑架构，但是我们还没有开始对bean加载功能的探索，之前提到过，bean的加载逻辑是在传入的ObjectFactory类型参数signletonFactory中定义的。我们回过头来看 AbstractBeanFactory#doGetBean 方法
 ```java
 	sharedInstance = getSingleton(beanName, () -> {
		try {
			return createBean(beanName, mbd, args);
		}
		catch (BeansException ex) {
			// Explicitly remove instance from singleton cache: It might have been put there
			// eagerly by the creation process, to allow for circular reference resolution.
			// Also remove any beans that received a temporary reference to the bean.
			destroySingleton(beanName);
			throw ex;
		}
	});
 ```
&emsp;&emsp;ObjectFactory的核心部分其实只是调用了createBean的方法，所以我们需要到createBean方法中去看下具体的实现。
## 5 准备创建bean
> AbstractAutowireCapableBeanFactory#createBean
 ```java
	@Override
	protected Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
			throws BeanCreationException {

		if (logger.isTraceEnabled()) {
			logger.trace("Creating instance of bean '" + beanName + "'");
		}
		RootBeanDefinition mbdToUse = mbd;

		// Make sure bean class is actually resolved at this point, and
		// clone the bean definition in case of a dynamically resolved Class
		// which cannot be stored in the shared merged bean definition.
		// 锁定class，根据设置的class属性或者根据className来解析Class
		Class<?> resolvedClass = resolveBeanClass(mbd, beanName);
		if (resolvedClass != null && !mbd.hasBeanClass() && mbd.getBeanClassName() != null) {
			mbdToUse = new RootBeanDefinition(mbd);
			mbdToUse.setBeanClass(resolvedClass);
		}

		// Prepare method overrides.
		//验证及准备覆盖的方法
		try {
			mbdToUse.prepareMethodOverrides();
		}
		catch (BeanDefinitionValidationException ex) {
			throw new BeanDefinitionStoreException(mbdToUse.getResourceDescription(),
					beanName, "Validation of method overrides failed", ex);
		}

		try {
			// Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
			//给 BeanPostProcessors 一个机会来返回代理来替代真正的bean
			Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
			if (bean != null) {
				return bean;
			}
		}
		catch (Throwable ex) {
			throw new BeanCreationException(mbdToUse.getResourceDescription(), beanName,
					"BeanPostProcessor before instantiation of bean failed", ex);
		}

		try {
			//创建bean
			Object beanInstance = doCreateBean(beanName, mbdToUse, args);
			if (logger.isTraceEnabled()) {
				logger.trace("Finished creating instance of bean '" + beanName + "'");
			}
			return beanInstance;
		}
		catch (BeanCreationException | ImplicitlyAppearedSingletonException ex) {
			// A previously detected exception with proper bean creation context already,
			// or illegal singleton state to be communicated up to DefaultSingletonBeanRegistry.
			throw ex;
		}
		catch (Throwable ex) {
			throw new BeanCreationException(
					mbdToUse.getResourceDescription(), beanName, "Unexpected exception during bean creation", ex);
		}
	}
 ```
&emsp;&emsp;上面的代码，主要做了以下完成了以下几个功能：
1. 根据设置的class属性或者根据className来解析Class。
2. 对override属性进行标记及验证。
	spring 配置的 lookup-method 和 replace-method ，就是统一存放在BeanDefinition中的methodOverrides属性中。
3. 应用初始化前的后处理器，解析指定bean是否存在初始化前的短路操作。
4. 创建bean

### 5.1 处理override属性
 ```java
public void prepareMethodOverrides() throws BeanDefinitionValidationException {
	// Check that lookup methods exist and determine their overloaded status.
	if (hasMethodOverrides()) {
		getMethodOverrides().getOverrides().forEach(this::prepareMethodOverride);
	}
}

protected void prepareMethodOverride(MethodOverride mo) throws BeanDefinitionValidationException {
	//获取对应类中的方法名的个数
	int count = ClassUtils.getMethodCountForName(getBeanClass(), mo.getMethodName());
	if (count == 0) {
		throw new BeanDefinitionValidationException(
				"Invalid method override: no method with name '" + mo.getMethodName() +
				"' on class [" + getBeanClassName() + "]");
	}
	else if (count == 1) {
		// Mark override as not overloaded, to avoid the overhead of arg type checking.
		//标记override未被覆盖，避免参数类型检查的开销
		mo.setOverloaded(false);
	}
}
 ```
&emsp;&emsp;之前提到过Spring配置中村子lookup-method和replace-method两个配置功能，而这两个配置的加载其实就是将配置统一存放在BeanDefinition中的methodOverrides属性里，这两个功能实现远离其实是bean实例化的时候如果检测到存在methodOverrides属性，会动态地为当前ben生成代理并使用对应的拦截器为bean做增强处理，相关逻辑实现在bean实例化部分详细介绍。
&emsp;&emsp;这里需要提到的是，对于方法的匹配来讲，如果一个类中存在若干个重载方法，那么，在函数调用即增强的时候还需要根据参数类型进行匹配，来最终确认当前调用的到底是哪个函数。但是，Spring将一部分匹配工作在这里完成了，如果当前类中的方法只有一个，那么就设置重载方法没有被重载，这样在后续调用的时候便可以直接使用找到的方法，而不是需要进行方法的参数匹配验证了，而且还可以提前对方法存在性进行验证。

### 5.2 实例化的前置处理
&emsp;&emsp;在真正调用doCreate方法创建bean的实例前使用了这样一个方法 resolveBeforeInstantiation(beanName, mbdToUse) 对 BeanDefinition 中的属性做些前置处理。当然，无论其中是否有相应的逻辑实现我们都可以理解，因为真正逻辑实现前后留有处理函数也是可扩展的一种体现，但是，这并不是最重要的，在函数中还提供了一个短路判断，这才是关键的部分。
 ```java
 	if (bean != null) {
		return bean;
	}
 ```
 &emsp;&emsp;当经过前置处理后返回的结果如果不为空，那么会直接略过后续的Bean的创建而直接返回结果。这一特点虽然很容易被忽略，但却起着至关重要的作用，我们熟知的AOP功能就是基于这里的判断的。
 ```java
@Nullable
protected Object resolveBeforeInstantiation(String beanName, RootBeanDefinition mbd) {
	Object bean = null;
	//如果 RootBeanDefinition 还未被解析
	if (!Boolean.FALSE.equals(mbd.beforeInstantiationResolved)) {
		// Make sure bean class is actually resolved at this point.
		if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
			Class<?> targetType = determineTargetType(beanName, mbd);
			if (targetType != null) {
				//实例化前的处理器
				bean = applyBeanPostProcessorsBeforeInstantiation(targetType, beanName);
				if (bean != null) {
					//实例化后的处理器
					bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
				}
			}
		}
		mbd.beforeInstantiationResolved = (bean != null);
	}
	return bean;
}
 ```
 &emsp;&emsp;此方法中最重要的无疑是两个方法 applyBeanPostProcessorsBeforeInstantiation 和 applyBeanPostProcessorsAfterInitialization 。这两个方法实现得非常简单，无非就是对处理器中的所有 InstantiationAwareBeanPostProcessor 类型的后处理器进行 postProcessBeforeInstantiation 和 BeanPostProcessor 的 postProcessAfterInitialization 方法的调用。

## 6 循环依赖
&emsp;&emsp;实例化bean是一个非常复杂的过程，其中比较难理解的的就是对循环依赖的解决，Spring又是如何解决循环依赖的呢？
1. 构造器循环依赖，此依赖是无法解决的，只能抛出BeanCurrentLyInCreationException异常表示循环依赖。
2. setter循环依赖，对于setter依赖注入造成的依赖是通过Spring容器提前暴露刚完成构造器注入但未完成其他补助的bean来完成的，而且只能解决单例作用域的bean循环依赖。通过提前暴露一个单例工厂方法，从而使其他bean能应用到该bean,代码如下所示：
 ```java
 addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
 ```
3. prototype 返回的依赖处理
&emsp;&emsp;对于“prototype”作用域bean，Spring容器无法完成依赖注入，因为Spring容器不进行缓存“prototype”作用域的bean，因此无法提前暴露一个创建中的bean。
之前我们在介绍加载bean的时候，已经看到对应的相关代码了
 ```java
	//只有在单例情况才会尝试解决循环依赖，原型模式情况下： A 属性中有B的属性，B属性中有A的属性，
	// 那么在依赖注入的时候，就会产生当A还未创建完成的时候因为对B的创建再次返回创建A，造成循环依赖
	// beforePrototypeCreation(beanName)
	if (isPrototypeCurrentlyInCreation(beanName)) {
		throw new BeanCurrentlyInCreationException(beanName);
	}
	//创建 prototype 域 bean 的处理 beforePrototypeCreation 会将当前创建的bean，加到对应的缓存中
	Object scopedInstance = scope.get(beanName, () -> {
							beforePrototypeCreation(beanName);
							try {
								return createBean(beanName, mbd, args);
							}
							finally {
								afterPrototypeCreation(beanName);
							}
						});
						bean = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
 ```

&emsp;&emsp;针对“singleton”作用域的bean，可以通过"setAllowCircularReferences(false)"来禁用循环依赖。

## 7 创建 bean
&emsp;&emsp;我们接着 createBean 方法来说。当经过 resolveBeforeInstantiation 方法后，程序有两个选择，如果创建了代理或者说重写了InstantiationAwareBeanPostProcessor 的 postProcessBeforeInstantiation 方法，并在方法 postProcessBeforeInstantiation 中改变了bean，这直接返回就可以了，否则需要进行常规的bean的创建。而常规的bean的创建时在doCreateBean中完成的。
<span id="doCreateBean"></span>

 ```java
 protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final @Nullable Object[] args)
			throws BeanCreationException {

		// Instantiate the bean.
		BeanWrapper instanceWrapper = null;
		if (mbd.isSingleton()) {
			instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
		}
		if (instanceWrapper == null) {
			//根据指定bean使用对应的策略创建新的实例，如：工厂方法，构造函数自动注入，简单初始化
			instanceWrapper = createBeanInstance(beanName, mbd, args);
		}
		final Object bean = instanceWrapper.getWrappedInstance();
		Class<?> beanType = instanceWrapper.getWrappedClass();
		if (beanType != NullBean.class) {
			mbd.resolvedTargetType = beanType;
		}

		// Allow post-processors to modify the merged bean definition.
		synchronized (mbd.postProcessingLock) {
			if (!mbd.postProcessed) {
				try {
					//应用MergedBeanDefinitionPostProcessor
					applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
				}
				catch (Throwable ex) {
					throw new BeanCreationException(mbd.getResourceDescription(), beanName,
							"Post-processing of merged bean definition failed", ex);
				}
				mbd.postProcessed = true;
			}
		}

		// Eagerly cache singletons to be able to resolve circular references
		// even when triggered by lifecycle interfaces like BeanFactoryAware.
		// 是否需要提前曝光：单例&允许循环依赖&当前bean正在创建中，检测循环依赖
		boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
				isSingletonCurrentlyInCreation(beanName));
		if (earlySingletonExposure) {
			if (logger.isTraceEnabled()) {
				logger.trace("Eagerly caching bean '" + beanName +
						"' to allow for resolving potential circular references");
			}
			/**
			 * addSingletonFactory 为了避免后期循环依赖可以在bean初始化完成前将创建实例的ObjectFactory加入工厂
			 * getEarlyBeanReference 对bean再一次依赖引用主要应用SmartInstantiationAware Bean PostProcessor
			 * 其中我们熟知的AOP就是在这里将advice动态植入bean中，若没有这直接返回bean，不做任何处理
			 */
			addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
		}

		// Initialize the bean instance.
		Object exposedObject = bean;
		try {
			//对bean进行填充，将各个属性值注入，其中，可能存在依赖其他bean的属性，则会递归初始化依赖bean
			populateBean(beanName, mbd, instanceWrapper);
			//调用初始化方法，比如init-method
			exposedObject = initializeBean(beanName, exposedObject, mbd);
		}
		catch (Throwable ex) {
			if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
				throw (BeanCreationException) ex;
			}
			else {
				throw new BeanCreationException(
						mbd.getResourceDescription(), beanName, "Initialization of bean failed", ex);
			}
		}

		if (earlySingletonExposure) {
			Object earlySingletonReference = getSingleton(beanName, false);
			//getEarlyBeanReference 只要检测到有循环依赖的情况下才会不为空
			if (earlySingletonReference != null) {
				//如果 exposedObject 没有在初始化方法中被改变，也就是没有被增强
				if (exposedObject == bean) {
					exposedObject = earlySingletonReference;
				}
				else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
					String[] dependentBeans = getDependentBeans(beanName);
					Set<String> actualDependentBeans = new LinkedHashSet<>(dependentBeans.length);
					for (String dependentBean : dependentBeans) {
						//检测循环依赖
						if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
							actualDependentBeans.add(dependentBean);
						}
					}
					/**
					 * 因为bean创建后其所依赖的bean一定是已经创建的，
					 * actualDependentBeans 不为空则表示当前 bean 创建后其依赖的 bean 却没有没全部创建完，也就是说存在循环依赖
					 */
					if (!actualDependentBeans.isEmpty()) {
						throw new BeanCurrentlyInCreationException(beanName,
								"Bean with name '" + beanName + "' has been injected into other beans [" +
								StringUtils.collectionToCommaDelimitedString(actualDependentBeans) +
								"] in its raw version as part of a circular reference, but has eventually been " +
								"wrapped. This means that said other beans do not use the final version of the " +
								"bean. This is often the result of over-eager type matching - consider using " +
								"'getBeanNamesOfType' with the 'allowEagerInit' flag turned off, for example.");
					}
				}
			}
		}

		// Register bean as disposable.
		try {
			// 根据scope注册bean
			registerDisposableBeanIfNecessary(beanName, bean, mbd);
		}
		catch (BeanDefinitionValidationException ex) {
			throw new BeanCreationException(
					mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);
		}

		return exposedObject;
	}
 ```
&emsp;&emsp;这个方法具体做了以下几件事：
1. 如果是单例则首先清除缓存。
2. 实例化bean，将 BeanDefinition 转换为 BeanWapper。
	转换是个复杂的过程，但是我们可以尝试概括大致的功能，如下：
	* 如果存在工厂方法则使用工厂方法进行初始化。
	* 一个类有多个构造函数，每个构造函数都有不同的参数，所以需要根据参数锁定结构函数并进行初始化。
	* 如果既不存在工厂方法也不存在带有参数的构造函数，则使用默认的构造函数进行bean实例化。
3. MergedBeanDefinitionPostProcessor的应用。
	bean合并后的处理，Autowired注解正式通过此方法实现诸如类型的预解析。
4. 依赖处理。
	在 Spring 中会有循环依赖的情况，例如，当 A 中含有 B 的属性，而 B 中又含有 A 的属性时就会构成一个循环依赖，此时如果 A 和 B 都是单例，那么在 Spring 中的处理方式就是当创建 B 的时候，涉及自动注入 A 的步骤时，并不是直接去再次创建 A，而是通过放入缓存中的 ObjectFactory 来创建实例，这样就解决了循环依赖的问题。
5. 属性填充。将所有属性填充至bean的实例中。
6. 循环依赖检查。
	之前有提到过，在 Sping 中解决循环依赖只对单例有效，而对于 prototype 的 bean, Spring 没有好的解决办法，唯一要做的就是抛出异常 。 在这个步骤里面会检测已经加载的 bean 是否 已经出现了依赖循环，并判断是再需要抛出异常 。
7. 注册DisposableBean。
	如果配置了 destroy-method，这里需要注册以便于在销毁时候调用。
8. 完成创建并返回。
	可以看到上面的步骤非常的繁琐，每一步骤都使用了大量的代码来完成其功能，最复杂也是最难以理解的当属循环依赖的处理，在真正进入 doCreateBean 前我们有必要先了解下循环依赖 。
#### 7.1 创建bean的实例
 ```java
 protected BeanWrapper createBeanInstance(String beanName, RootBeanDefinition mbd, @Nullable Object[] args) {
		// Make sure bean class is actually resolved at this point.
		Class<?> beanClass = resolveBeanClass(mbd, beanName);

		if (beanClass != null && !Modifier.isPublic(beanClass.getModifiers()) && !mbd.isNonPublicAccessAllowed()) {
			throw new BeanCreationException(mbd.getResourceDescription(), beanName,
					"Bean class isn't public, and non-public access not allowed: " + beanClass.getName());
		}

		Supplier<?> instanceSupplier = mbd.getInstanceSupplier();
		//Supplier 接口 方法 初始化
		if (instanceSupplier != null) {
			return obtainFromSupplier(instanceSupplier, beanName);
		}

		//如果工厂方法不为空则使用工厂方法初始化策略
		if (mbd.getFactoryMethodName() != null) {
			return instantiateUsingFactoryMethod(beanName, mbd, args);
		}

		// Shortcut when re-creating the same bean...
		boolean resolved = false;
		boolean autowireNecessary = false;
		if (args == null) {
			synchronized (mbd.constructorArgumentLock) {
				//一个类有多个构造函数，每个构造函数都有不同的参数，所以调用前需要先根据参数锁定构造函数或对应的工厂方法
				if (mbd.resolvedConstructorOrFactoryMethod != null) {
					resolved = true;
					autowireNecessary = mbd.constructorArgumentsResolved;
				}
			}
		}
		//如果已经解析好的构造函数方法不需要再次锁定
		if (resolved) {
			if (autowireNecessary) {
				//构造函数自动注入
				return autowireConstructor(beanName, mbd, null, null);
			}
			else {
				//使用默认的构造函数构造
				return instantiateBean(beanName, mbd);
			}
		}

		// Candidate constructors for autowiring?
		//需要根据参数解析构造函数
		Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
		if (ctors != null || mbd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR ||
				mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args)) {
			//构造函数自动注入
			return autowireConstructor(beanName, mbd, ctors, args);
		}

		// Preferred constructors for default construction?
		ctors = mbd.getPreferredConstructors();
		if (ctors != null) {
			//构造函数自动注入
			return autowireConstructor(beanName, mbd, ctors, null);
		}

		// No special handling: simply use no-arg constructor.
		//使用默认构造函数构造
		return instantiateBean(beanName, mbd);
	}

 ```
&emsp;&emsp;从上面的代码中可以看到细节实现非常复杂，但是我们也可以清晰地看到实例化的逻辑。
1. 首先RootBeanDefinition中查找Supplier接口，如果存在则使用Supplier创建bean。
2. 然后查找RootBeanDefinition是否存在factoryMethodName属性，或者说在配置文件中配置了factory-method，那么Spring会尝试使用 instantiateUsingFactoryMethod(beanName, mbd, args) 方法，根据RootBeanDefinition配置生成bean的实例。
3. 解析构造函数，并进行构造函数的实例化。因为一个bena对应的类中可能会有多个构造函数，而每个构造函数的参数不同，Spring在根据参数及类型去判断最终会使用哪个构造函数进行实话。但是判断过程是一个比较消耗性能的步骤，所以采用缓存机制，如果已经解析过着不需要重复解析而直接从RootBeanDefinition中的属性resolvedConstructorOrFactoryMethod缓存的值去取、否则需要再次解析，并将解析结果添加至 RootBeanDefinition 中的属性 resolvedConstructorOrFactoryMethod 中。

### 7.1.1 ConstructorResolver#autowiredConstructor
 ```java
 	public BeanWrapper autowireConstructor(String beanName, RootBeanDefinition mbd,
			@Nullable Constructor<?>[] chosenCtors, @Nullable Object[] explicitArgs) {

		BeanWrapperImpl bw = new BeanWrapperImpl();
		this.beanFactory.initBeanWrapper(bw);

		Constructor<?> constructorToUse = null;
		ArgumentsHolder argsHolderToUse = null;
		Object[] argsToUse = null;

		//explicitArgs 通过 getBean 方法传入
		if (explicitArgs != null) {
			argsToUse = explicitArgs;
		}
		else {
			//如果在getBean方法的时候没有指定则尝试从配置文件中解析
			Object[] argsToResolve = null;
			synchronized (mbd.constructorArgumentLock) {
				constructorToUse = (Constructor<?>) mbd.resolvedConstructorOrFactoryMethod;
				//判断缓存中是否存在已解析的构造函数，若有则从中缓存中取
				if (constructorToUse != null && mbd.constructorArgumentsResolved) {
					// Found a cached constructor...
					argsToUse = mbd.resolvedConstructorArguments;
					if (argsToUse == null) {
						//配置构造参数
						argsToResolve = mbd.preparedConstructorArguments;
					}
				}
			}
			//如果缓存中存在
			if (argsToResolve != null) {
				//解析参数类型，如果给定方法的构造函数A(int,int)则通过此方法后就会把配置文件中的("1","1")转换为(1,1)
				//缓存中的值可能是原始值，也可能是最终值
				argsToUse = resolvePreparedArguments(beanName, mbd, bw, constructorToUse, argsToResolve, true);
			}
		}

		//没有缓存
		if (constructorToUse == null || argsToUse == null) {
			// Take specified constructors, if any.
			Constructor<?>[] candidates = chosenCtors;
			if (candidates == null) {
				Class<?> beanClass = mbd.getBeanClass();
				try {
					candidates = (mbd.isNonPublicAccessAllowed() ?
							beanClass.getDeclaredConstructors() : beanClass.getConstructors());
				}
				catch (Throwable ex) {
					throw new BeanCreationException(mbd.getResourceDescription(), beanName,
							"Resolution of declared constructors on bean Class [" + beanClass.getName() +
							"] from ClassLoader [" + beanClass.getClassLoader() + "] failed", ex);
				}
			}

			//根据配置获取到的构造方法，判断无构造参数的处理
			if (candidates.length == 1 && explicitArgs == null && !mbd.hasConstructorArgumentValues()) {
				Constructor<?> uniqueCandidate = candidates[0];
				if (uniqueCandidate.getParameterCount() == 0) {
					synchronized (mbd.constructorArgumentLock) {
						mbd.resolvedConstructorOrFactoryMethod = uniqueCandidate;
						mbd.constructorArgumentsResolved = true;
						mbd.resolvedConstructorArguments = EMPTY_ARGS;
					}
					//无参数构造函数创建实例，instantiate 采用策略方式处理，当配置中存在 overrideMethods 时，通过Cglib进行增强
					bw.setBeanInstance(instantiate(beanName, mbd, uniqueCandidate, EMPTY_ARGS));
					return bw;
				}
			}

			// Need to resolve the constructor.
			//解析构造函数，当传入的constructor不为空，或 注入方式为构造函数注入
			boolean autowiring = (chosenCtors != null ||
					mbd.getResolvedAutowireMode() == AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR);
			ConstructorArgumentValues resolvedValues = null;

			int minNrOfArgs;
			if (explicitArgs != null) {
				minNrOfArgs = explicitArgs.length;
			}
			else {
				//根据配置文件中的配置的构造函数参数
				ConstructorArgumentValues cargs = mbd.getConstructorArgumentValues();
				//用于承载解析后的构造函数参数值
				resolvedValues = new ConstructorArgumentValues();
				//能解析到的参数个数,同时处理依赖
				minNrOfArgs = resolveConstructorArguments(beanName, mbd, bw, cargs, resolvedValues);
			}
			//排序给定的构造函数，public 构造函数优先， 然后根据参数数量降序
			AutowireUtils.sortConstructors(candidates);
			int minTypeDiffWeight = Integer.MAX_VALUE;
			Set<Constructor<?>> ambiguousConstructors = null;
			LinkedList<UnsatisfiedDependencyException> causes = null;

			for (Constructor<?> candidate : candidates) {

				int parameterCount = candidate.getParameterCount();

				//如果已经找到选用构造函数
				if (constructorToUse != null && argsToUse != null && argsToUse.length > parameterCount) {
					// Already found greedy constructor that can be satisfied ->
					// do not look any further, there are only less greedy constructors left.
					break;
				}
				//获取到当前class的构造函数 小于  根据配置参数解析出的个数或者获取 getBean 传入的期望的参数类型数量
				if (parameterCount < minNrOfArgs) {
					continue;
				}

				ArgumentsHolder argsHolder;
				Class<?>[] paramTypes = candidate.getParameterTypes();
				if (resolvedValues != null) {
					try {
						//通过 ConstructorProperties 注解 获取参数名
						String[] paramNames = ConstructorPropertiesChecker.evaluate(candidate, parameterCount);
						if (paramNames == null) {
							//通过默认解析器，解析出参数名称
							ParameterNameDiscoverer pnd = this.beanFactory.getParameterNameDiscoverer();
							if (pnd != null) {
								paramNames = pnd.getParameterNames(candidate);
							}
						}
						//根据名称和数据类型创建参数持有者
						argsHolder = createArgumentArray(beanName, mbd, resolvedValues, bw, paramTypes, paramNames,
								getUserDeclaredConstructor(candidate), autowiring, candidates.length == 1);
					}
					catch (UnsatisfiedDependencyException ex) {
						if (logger.isTraceEnabled()) {
							logger.trace("Ignoring constructor [" + candidate + "] of bean '" + beanName + "': " + ex);
						}
						// Swallow and try next constructor.
						if (causes == null) {
							causes = new LinkedList<>();
						}
						causes.add(ex);
						continue;
					}
				}
				// resolvedValues 为空，getBean 传递了构造函数参数类型
				else {
					// Explicit arguments given -> arguments length must match exactly.
					if (parameterCount != explicitArgs.length) {
						continue;
					}
					argsHolder = new ArgumentsHolder(explicitArgs);
				}
				//解析是否有不确定性的构造函数存在，例如不同构造函数的参数为父子关系
				int typeDiffWeight = (mbd.isLenientConstructorResolution() ?
						argsHolder.getTypeDifferenceWeight(paramTypes) : argsHolder.getAssignabilityWeight(paramTypes));
				// Choose this constructor if it represents the closest match.
				//如果它代表这当前最接近的匹配这选择作为构造函数，如果类型相同这为0，类型不相同，没有父子关系，这为 Integer.MAX_VALUE
				if (typeDiffWeight < minTypeDiffWeight) {
					constructorToUse = candidate;
					argsHolderToUse = argsHolder;
					argsToUse = argsHolder.arguments;
					minTypeDiffWeight = typeDiffWeight;
					ambiguousConstructors = null;
				}
				else if (constructorToUse != null && typeDiffWeight == minTypeDiffWeight) {
					if (ambiguousConstructors == null) {
						ambiguousConstructors = new LinkedHashSet<>();
						ambiguousConstructors.add(constructorToUse);
					}
					ambiguousConstructors.add(candidate);
				}
			}

			if (constructorToUse == null) {
				if (causes != null) {
					UnsatisfiedDependencyException ex = causes.removeLast();
					for (Exception cause : causes) {
						this.beanFactory.onSuppressedException(cause);
					}
					throw ex;
				}
				throw new BeanCreationException(mbd.getResourceDescription(), beanName,
						"Could not resolve matching constructor " +
						"(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities)");
			}
			else if (ambiguousConstructors != null && !mbd.isLenientConstructorResolution()) {
				throw new BeanCreationException(mbd.getResourceDescription(), beanName,
						"Ambiguous constructor matches found in bean '" + beanName + "' " +
						"(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities): " +
						ambiguousConstructors);
			}

			if (explicitArgs == null && argsHolderToUse != null) {
				argsHolderToUse.storeCache(mbd, constructorToUse);
			}
		}

		Assert.state(argsToUse != null, "Unresolved constructor arguments");
		//将构建的实例缓存到bw中
		bw.setBeanInstance(instantiate(beanName, mbd, constructorToUse, argsToUse));
		return bw;
	}
 ```
&emsp;&emsp;这段代码，逻辑复杂，其中的变量都有其相关的作用。咱们简单说明一下该函数的几个功能方面。
1. 构造函数参数的确定。
* 根据 explicitArgs 参数判断。
&emsp;&emsp;如果传入的参数explicitArgs不为空，那么可以直接确定参数，因为explicitArgs参数是在调用Bean的时候指定的，在BeanFactory类中存在这样的方法：
> public Object getBean(String name, Object... args) throws BeansException;

在获取bean的时候，用户不但可以指定bean的名称还可以指定bean所对应的类的构造函数或者工厂方法的方法参数，主要用于静态工厂方法的调用，而这里需要给定构造函数参数就是它。
* **缓存中获取**
&emsp;&emsp;确定参数的办法，如果之前已经分析过，也就是说构造函数参数已经记录在缓存中，那么便可以直接拿来使用。而且，这里要提到的是，在缓存中缓存的可能是参数的最终类型也可能是参数的初始类型，例如:构造函数参数要求的是 int 类型，但是原始的参数值可能 是 String类型的“1”，那么 即使在缓存中得到了参数，也需要经过类型转换器的过滤以确保参数类型与对应的构造函数参数类型完全对应 。
* **配置获取**
&emsp;&emsp;如果不能根据传人的参数explicitArgs确定构造函数的参数也无法在缓存中得到相关信息，那么只能开始新一轮的分析了。 
分析从获取配置文件中配置的构造函数信息开始，经过之前的分析，我们知道， Spring 中配置文件中的信息经过转换都会通过 BeanDefinition 实例承载，也就是参数 mbd 中包含，那么 可以通过调用 mbd.getConstructorArgumentValues() 来获取配置的构造函数信息。 有了配置中的 信息便可以获取对应的参数值信息了，获取参数值的信息包括直接指定值，如:直接指定构造 函数中某个值为原始类型 String 类型，或者是 一个对其他 bean 的引用，而这一处理委托给 resoIveConstructorArguments 方法，并返回能解析到的参数的个数 。

2. 构造函数的确认
&emsp;&emsp;经过第一步已经确定经过了第一步后已经确定了构造函数的参数，接下来的任务就是根据构造函数参数在所有 构造函数中锁定对应的构造函数，而匹配的方法就是根据参数个数匹配，所以在匹配之前需要先对构造函数按照 public构造函数优先参数数量降序、 非public构造函数参数数量降序。 这样 可以在遍历的情况下迅速判断排在后面的构造函数参数个数是否符合条件 。
由于在配置文件中并不是唯一限制使用参数位置索引的方式去创建，同样还支持指定参数 名称进行设定参数值的情况，如<constructor-arg name=”aa">，那么这种情况就需要首先确定构 造函数中的参数名称 。
获取参数名称可以有两种方式，一种是通过注解的方式直接获取 ， 另一种就是使用 Spring 中提供的工具类 ParameterNameDiscoverer 来获取。 构造函数、参数名称、参数类型、参数值 都确定后就可以锁定构造函数以及转换对应的参数类型了 。
3. 根据确定的构造函数转换对应的参数类型 。
主要是使用 Spring 中提供的类型转换器或者用户提供的自定义类型转换器进行转换 。
4. 构造函数不确定性的验证 。
当然，有时候即使构造函数、参数名称、参数类型 、参数值都确定后也不一定会直接锁定
构造函数，不同构造函数的参数为父子关系，所以 Spring在最后又做了一次验证。
5. 根据实例化策略以及得到的构造函数及构造函数参数实例化 Bean。 后面章节中将进行讲解 。

### 7.1.2 AbstractAutowireCapableBeanFactory#instantiateBean
&emsp;&emsp;不带构造参数的构造函数的实例化过程,直接调用实例化策略进行实例化就可以了
 ```java
 	protected BeanWrapper instantiateBean(final String beanName, final RootBeanDefinition mbd) {
		try {
			Object beanInstance;
			final BeanFactory parent = this;
			if (System.getSecurityManager() != null) {
				beanInstance = AccessController.doPrivileged((PrivilegedAction<Object>) () ->
						getInstantiationStrategy().instantiate(mbd, beanName, parent),
						getAccessControlContext());
			}
			else {
				beanInstance = getInstantiationStrategy().instantiate(mbd, beanName, parent);
			}
			BeanWrapper bw = new BeanWrapperImpl(beanInstance);
			initBeanWrapper(bw);
			return bw;
		}
		catch (Throwable ex) {
			throw new BeanCreationException(
					mbd.getResourceDescription(), beanName, "Instantiation of bean failed", ex);
		}
	}
 ```
### 7.1.3 实例化策略
&emsp;&emsp;实例化策略是什么了，又可以做什么？经过前面的分析，我们已经得到了足以实例化的所有相关的信息，完全可以使用最简单的反射方法直接反射来构造实例对象，但是Spring却并没有这么做。
&emsp;&emsp;AbstractAutowireCapableBeanFactory中默认的 InstantiationStrategy 策略是 CglibSubclassingInstantiationStrategy，而其又是继承 SimpleInstantiationStrategy。
在上面的创建实例的过程中，我们已经看到， getInstantiationStrategy().instantiate(mbd, beanName, parent) 该方法。接下来我们看看 instantiate(mbd, beanName, parent) 方法
>SimpleInstantiationStrategy#instantiate
 ```java
 	@Override
	public Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner,
			final Constructor<?> ctor, Object... args) {
	
		if (!bd.hasMethodOverrides()) {
			if (System.getSecurityManager() != null) {
				// use own privileged to change accessibility (when security is on)
				AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
					ReflectionUtils.makeAccessible(ctor);
					return null;
				});
			}
			return BeanUtils.instantiateClass(ctor, args);
		}
		else {
			return instantiateWithMethodInjection(bd, beanName, owner, ctor, args);
		}
	}
 ```
>CglibSubclassingInstantiationStrategy.java
 ```java
 	@Override
	protected Object instantiateWithMethodInjection(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner,
			@Nullable Constructor<?> ctor, Object... args) {

		// Must generate CGLIB subclass...
		return new CglibSubclassCreator(bd, owner).instantiate(ctor, args);
	}

private static class CglibSubclassCreator {
	//..省略其他代码.//
	public Object instantiate(@Nullable Constructor<?> ctor, Object... args) {
		Class<?> subclass = createEnhancedSubclass(this.beanDefinition);
		Object instance;
		if (ctor == null) {
			instance = BeanUtils.instantiateClass(subclass);
		}
		else {
			try {
				Constructor<?> enhancedSubclassConstructor = subclass.getConstructor(ctor.getParameterTypes());
				instance = enhancedSubclassConstructor.newInstance(args);
			}
			catch (Exception ex) {
				throw new BeanInstantiationException(this.beanDefinition.getBeanClass(),
						"Failed to invoke constructor for CGLIB enhanced subclass [" + subclass.getName() + "]", ex);
			}
		}
		// SPR-10785: set callbacks directly on the instance instead of in the
		// enhanced class (via the Enhancer) in order to avoid memory leaks.
		Factory factory = (Factory) instance;
		factory.setCallbacks(new Callback[] {NoOp.INSTANCE,
				new LookupOverrideMethodInterceptor(this.beanDefinition, this.owner),
				new ReplaceOverrideMethodInterceptor(this.beanDefinition, this.owner)});
		return instance;
	}
	private Class<?> createEnhancedSubclass(RootBeanDefinition beanDefinition) {
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(beanDefinition.getBeanClass());
		enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
		if (this.owner instanceof ConfigurableBeanFactory) {
			ClassLoader cl = ((ConfigurableBeanFactory) this.owner).getBeanClassLoader();
			enhancer.setStrategy(new ClassLoaderAwareGeneratorStrategy(cl));
		}
		enhancer.setCallbackFilter(new MethodOverrideCallbackFilter(beanDefinition));
		enhancer.setCallbackTypes(CALLBACK_TYPES);
		return enhancer.createClass();
	}
}
 ```
&emsp;&emsp;看到这里我们已经可以知道使用replace或者lookup的配置方法是如何实现了。因为需要将这两个配置提供的功能切入进去，所以就必须使用动态代理的方式将包含2个特性所对应的逻辑的拦截增强器设置进去，这样才可以保证在调用方法的时候会被相应的拦截器增强，返回值为包含拦截器的代理实例。
&emsp;&emsp;对于拦截器的处理方法非常简单，不再详细介绍，如果又读者有兴趣，可以仔细看看关于AOP的介绍。

## 7.2 记录创建bean的ObjectFactory
&emsp;&emsp;在 [doCreateBean](#doCreateBean) 函数中有这样一段代码:
 ```java
 	// Eagerly cache singletons to be able to resolve circular references
	// even when triggered by lifecycle interfaces like BeanFactoryAware.
	// 是否需要提前曝光：单例&允许循环依赖&当前bean正在创建中，检测循环依赖
	boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
			isSingletonCurrentlyInCreation(beanName));
	if (earlySingletonExposure) {
		if (logger.isTraceEnabled()) {
			logger.trace("Eagerly caching bean '" + beanName +
					"' to allow for resolving potential circular references");
		}
		/**
			* addSingletonFactory 为了避免后期循环依赖可以在bean初始化完成前将创建实例的ObjectFactory加入工厂
			* getEarlyBeanReference 对bean再一次依赖引用主要应用SmartInstantiationAware Bean PostProcessor
			* 其中我们熟知的AOP就是在这里将advice动态植入bean中，若没有这直接返回bean，不做任何处理
			*/
		addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
	}
 ```
&emsp;&emsp;我们之前提到过循环依赖的问题，Spring循环依赖的解决办法，是提前让创建的bean进行提前曝光。

* earlySingletonExposure: 从字面的意思理解就是提早曝光的单例，我们暂不定义它的学名叫什么，我们感兴趣的是有哪些条件影响这个值 。 
* mbd.isSingleton():没有太多可以解释的，此RootBeanDefinition代表的是否是单例。 
* this.allowCircularReferences:是否九许循环依赖，很抱歉，并没有找到在配直文件中如何配直，但是在 AbstractRefreshableApplicationContext 中提供了设直函数，可以通 过硬编码的方式进行设直或者可以通过自定义命名空间进行配置，其中硬编码的方式代码如下 。
 ```java
	ClassPathXmlApplicationContext bf = new ClassPathXmlApplicationContext ("aspectTest.xml") ;
	bf.setAllOwBeanDefinitionOverriding(false);
 ```
* isSingletonCurrentlyInCreation(beanName):该bean是否在创建中。在Spring中，会有一个专门的属性默认为DefaultSingletonBeanRegistry的singletonsCurrentLyInCreation来记录bean的加载状态，在bean开始创建前会将beanName记录在属性中，在bean创建结束后将 beanName 从属性中移除。那么我们跟随代码一路走来可是对这个属性的记录没有都说好印象，这个状态是在那里记录的呢？不通scope的记录位置并不一样，我们以singleton为例，在 singleton 下记录属性的函数是在 DefaultSingletonBeanRegistry 类的 public Object getSingleton(String beanName, ObjectFactory singletonFactory) 函数的 beaforeSingletonCreation(beanName) 和 afterSingletonCreation(beanName)中，在这两段函数中分别 this.singletonsCurrentLyInCreation.add(beanName)与 this.singletonsCurrentlyInCreation.remove(beanName)来记录状态的记录与移除。

## 7.3 属性注入
&emsp;&emsp;接下来我们看下属性注入，在Spring创建了对象实例后，就是针对bean进行属性填充
 ```java
	@SuppressWarnings("deprecation")  // for postProcessPropertyValues
	protected void populateBean(String beanName, RootBeanDefinition mbd, @Nullable BeanWrapper bw) {
		if (bw == null) {
			if (mbd.hasPropertyValues()) {
				throw new BeanCreationException(
						mbd.getResourceDescription(), beanName, "Cannot apply property values to null instance");
			}
			else {
				// Skip property population phase for null instance.
				return;
			}
		}

		// Give any InstantiationAwareBeanPostProcessors the opportunity to modify the
		// state of the bean before properties are set. This can be used, for example,
		// to support styles of field injection.
		// 给 InstantiationAwareBeanPostProcessors 最后一次机会在属性设置来改变bean
		// 如：可以用来支持属性注入的类型
		if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
			for (BeanPostProcessor bp : getBeanPostProcessors()) {
				if (bp instanceof InstantiationAwareBeanPostProcessor) {
					InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
					//返回值为是否继续填充bean
					if (!ibp.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)) {
						return;
					}
				}
			}
		}

		PropertyValues pvs = (mbd.hasPropertyValues() ? mbd.getPropertyValues() : null);

		int resolvedAutowireMode = mbd.getResolvedAutowireMode();
		if (resolvedAutowireMode == AUTOWIRE_BY_NAME || resolvedAutowireMode == AUTOWIRE_BY_TYPE) {
			MutablePropertyValues newPvs = new MutablePropertyValues(pvs);
			// Add property values based on autowire by name if applicable.
			//根据名称自动注入
			if (resolvedAutowireMode == AUTOWIRE_BY_NAME) {
				autowireByName(beanName, mbd, bw, newPvs);
			}
			// Add property values based on autowire by type if applicable.
			//根据类型自动注入
			if (resolvedAutowireMode == AUTOWIRE_BY_TYPE) {
				autowireByType(beanName, mbd, bw, newPvs);
			}
			pvs = newPvs;
		}

		//后置处理器已经初始化
		boolean hasInstAwareBpps = hasInstantiationAwareBeanPostProcessors();
		//需要依赖检查
		boolean needsDepCheck = (mbd.getDependencyCheck() != AbstractBeanDefinition.DEPENDENCY_CHECK_NONE);

		PropertyDescriptor[] filteredPds = null;
		if (hasInstAwareBpps) {
			if (pvs == null) {
				pvs = mbd.getPropertyValues();
			}
			for (BeanPostProcessor bp : getBeanPostProcessors()) {
				if (bp instanceof InstantiationAwareBeanPostProcessor) {
					InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
					PropertyValues pvsToUse = ibp.postProcessProperties(pvs, bw.getWrappedInstance(), beanName);
					if (pvsToUse == null) {
						if (filteredPds == null) {
							filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
						}
						pvsToUse = ibp.postProcessPropertyValues(pvs, filteredPds, bw.getWrappedInstance(), beanName);
						if (pvsToUse == null) {
							return;
						}
					}
					pvs = pvsToUse;
				}
			}
		}
		if (needsDepCheck) {
			if (filteredPds == null) {
				filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
			}
			//依赖检查，对应depends-on属性
			checkDependencies(beanName, mbd, filteredPds, pvs);
		}

		if (pvs != null) {
			//将属性应用到bean中
			applyPropertyValues(beanName, mbd, bw, pvs);
		}
	}
 ```
&emsp;&emsp;在populateBean函数中提供了这样的处理流程。
1. InstantiationAwareBeanPostProcssor 处理器的 postProcessAfterInstantiation 函数应用，此函数可以控制程序是否继续进行属性填充。
2. 根据 注人类型( byName/byType )，提取依赖的 bean，并统一存入 PropertyValues 中 。
3. 应用 InstantiationAwareBeanPostProcessor处理器的 postProcessPropetyValues方法，对属性获取完毕填充前对属性的再次处理，典型应用是 RequiredAnnotationBeanPostProcessor 类中对属性的验证。
4. 将所有 PropertyValues 中的属性填充至 BeanWrapper 中。
### 7.3.1 autowireByName
 ```java
 	protected void autowireByName(
			String beanName, AbstractBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {
		//寻找bw中需要注入的属性
		String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
		for (String propertyName : propertyNames) {
			if (containsBean(propertyName)) {
				//递归初始化相关的bean
				Object bean = getBean(propertyName);
				pvs.add(propertyName, bean);
				//注册依赖
				registerDependentBean(propertyName, beanName);
				if (logger.isTraceEnabled()) {
					logger.trace("Added autowiring by name from bean name '" + beanName +
							"' via property '" + propertyName + "' to bean named '" + propertyName + "'");
				}
			}
			else {
				if (logger.isTraceEnabled()) {
					logger.trace("Not autowiring property '" + propertyName + "' of bean '" + beanName +
							"' by name: no matching bean found");
				}
			}
		}
	}
 ```
&emsp;&emsp;这个函数其实就是在传入参数pvs中找出已经加载的bean，并递归实例化，今儿加入到pvs中。

### 7.3.2 autowireByType
&emsp;&emsp;autowireByType与autowireByName 对于我们理解与使用来说复杂程度都很相似，但是其实现功能的复杂程度却完全不一样。
 ```java
 protected void autowireByType(
			String beanName, AbstractBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {

		TypeConverter converter = getCustomTypeConverter();
		if (converter == null) {
			converter = bw;
		}

		Set<String> autowiredBeanNames = new LinkedHashSet<>(4);
		//寻找bw中需要依赖注入的属性
		String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
		for (String propertyName : propertyNames) {
			try {
				PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
				// Don't try autowiring by type for type Object: never makes sense,
				// even if it technically is a unsatisfied, non-simple property.
				if (Object.class != pd.getPropertyType()) {
					//探测指定属性的 set 方法
					MethodParameter methodParam = BeanUtils.getWriteMethodParameter(pd);
					// Do not allow eager init for type matching in case of a prioritized post-processor.
					boolean eager = !(bw.getWrappedInstance() instanceof PriorityOrdered);
					DependencyDescriptor desc = new AutowireByTypeDependencyDescriptor(methodParam, eager);
					// 解析指定beanName的属性说匹配的值，并把解析到的属性名称储存在autowiredBeanNames中，当属性存在多个封装bean时
					// 如：@Autowired private List<A> aList;将会找到所有匹配A类型的bean并将其注入
					Object autowiredArgument = resolveDependency(desc, beanName, autowiredBeanNames, converter);
					if (autowiredArgument != null) {
						pvs.add(propertyName, autowiredArgument);
					}
					for (String autowiredBeanName : autowiredBeanNames) {
						//注册依赖
						registerDependentBean(autowiredBeanName, beanName);
						if (logger.isTraceEnabled()) {
							logger.trace("Autowiring by type from bean name '" + beanName + "' via property '" +
									propertyName + "' to bean named '" + autowiredBeanName + "'");
						}
					}
					autowiredBeanNames.clear();
				}
			}
			catch (BeansException ex) {
				throw new UnsatisfiedDependencyException(mbd.getResourceDescription(), beanName, propertyName, ex);
			}
		}
	}
 ```
&emsp;&emsp;实现根据名称自动匹配的第一步就是寻找bw中需要依赖注入的属性，同样对于根据类型自动匹配的实现来讲第一步就是寻找bw中需要依赖注入的属性，然后遍历这些属性并寻找类型匹配的bean，其中最复杂的就是寻找类型匹配的bean。同时，Spring中提供了对集合的类型注入的支持，如使用注解方式:
 ```java
 @Autowired
 private List<Test> tests;
 ```
&emsp;&emsp;Spring 将会把所有与 Test 匹配的类型找出来并注入到 tests 属性中，正是由于这一因素， 所以在 autowireByType 函数中，新建了局部遍历 autowiredBeanNames，用于存储所有依赖的 bean，如果只是对非集合类的属性注入来说，此属性并无用处 。
&emsp;&emsp;对于寻找类型匹配的逻辑实现封装在了resoveDependency函数中。
> DefaultListableBeanFactory 
 ```java
 	@Override
	@Nullable
	public Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName,
			@Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) throws BeansException {

		descriptor.initParameterNameDiscovery(getParameterNameDiscoverer());
		if (Optional.class == descriptor.getDependencyType()) {
			return createOptionalDependency(descriptor, requestingBeanName);
		}
		else if (ObjectFactory.class == descriptor.getDependencyType() ||
				ObjectProvider.class == descriptor.getDependencyType()) {
			//ObjectFactory/ObjectProvider 类注入的特殊处理
			return new DependencyObjectProvider(descriptor, requestingBeanName);
		}
		else if (javaxInjectProviderClass == descriptor.getDependencyType()) {
			//javaxInjectProviderClass 类注入的特殊处理
			return new Jsr330Factory().createDependencyProvider(descriptor, requestingBeanName);
		}
		else {
			//通用处理逻辑
			Object result = getAutowireCandidateResolver().getLazyResolutionProxyIfNecessary(
					descriptor, requestingBeanName);
			if (result == null) {
				result = doResolveDependency(descriptor, requestingBeanName, autowiredBeanNames, typeConverter);
			}
			return result;
		}
	}

 	@Nullable
	public Object doResolveDependency(DependencyDescriptor descriptor, @Nullable String beanName,
			@Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) throws BeansException {
		//获取前一个 InjectionPoint ，并将当前的 DependencyDescriptor 设置进去
		InjectionPoint previousInjectionPoint = ConstructorResolver.setCurrentInjectionPoint(descriptor);
		try {
			//先通过 DependencyDescriptor 的 resolveShortcut 方法 为给定工厂解析此依赖关系的快捷方式，例如考虑一些预先解析的信息
			Object shortcut = descriptor.resolveShortcut(this);
			if (shortcut != null) {
				return shortcut;
			}

			//获取 依赖 类型
			Class<?> type = descriptor.getDependencyType();

			/**
			 * 用于支持Spring新增的注解@Value
			 */
			Object value = getAutowireCandidateResolver().getSuggestedValue(descriptor);
			if (value != null) {
				if (value instanceof String) {
					String strVal = resolveEmbeddedValue((String) value);
					BeanDefinition bd = (beanName != null && containsBean(beanName) ?
							getMergedBeanDefinition(beanName) : null);
					value = evaluateBeanDefinitionString(strVal, bd);
				}
				TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
				try {
					return converter.convertIfNecessary(value, type, descriptor.getTypeDescriptor());
				}
				catch (UnsupportedOperationException ex) {
					// A custom TypeConverter which does not support TypeDescriptor resolution...
					return (descriptor.getField() != null ?
							converter.convertIfNecessary(value, type, descriptor.getField()) :
							converter.convertIfNecessary(value, type, descriptor.getMethodParameter()));
				}
			}

			//如果未解析成功，这考虑处理多重嵌套bean，例如Stream/Array/Collections/Map
			Object multipleBeans = resolveMultipleBeans(descriptor, beanName, autowiredBeanNames, typeConverter);
			if (multipleBeans != null) {
				return multipleBeans;
			}

			//解析对象
			Map<String, Object> matchingBeans = findAutowireCandidates(beanName, type, descriptor);
			//如果 autowire 的 require 属性为 true 而找到的匹配项却为空则只能抛出异常
			if (matchingBeans.isEmpty()) {
				if (isRequired(descriptor)) {
					raiseNoMatchingBeanFound(type, descriptor.getResolvableType(), descriptor);
				}
				return null;
			}

			String autowiredBeanName;
			Object instanceCandidate;

			if (matchingBeans.size() > 1) {
				autowiredBeanName = determineAutowireCandidate(matchingBeans, descriptor);
				if (autowiredBeanName == null) {
					if (isRequired(descriptor) || !indicatesMultipleBeans(type)) {
						return descriptor.resolveNotUnique(descriptor.getResolvableType(), matchingBeans);
					}
					else {
						// In case of an optional Collection/Map, silently ignore a non-unique case:
						// possibly it was meant to be an empty collection of multiple regular beans
						// (before 4.3 in particular when we didn't even look for collection beans).
						return null;
					}
				}
				instanceCandidate = matchingBeans.get(autowiredBeanName);
			}
			else {
				// We have exactly one match.
				Map.Entry<String, Object> entry = matchingBeans.entrySet().iterator().next();
				autowiredBeanName = entry.getKey();
				instanceCandidate = entry.getValue();
			}

			if (autowiredBeanNames != null) {
				autowiredBeanNames.add(autowiredBeanName);
			}
			if (instanceCandidate instanceof Class) {
				instanceCandidate = descriptor.resolveCandidate(autowiredBeanName, type, this);
			}
			Object result = instanceCandidate;
			if (result instanceof NullBean) {
				if (isRequired(descriptor)) {
					raiseNoMatchingBeanFound(type, descriptor.getResolvableType(), descriptor);
				}
				result = null;
			}
			if (!ClassUtils.isAssignableValue(type, result)) {
				throw new BeanNotOfRequiredTypeException(autowiredBeanName, type, instanceCandidate.getClass());
			}
			return result;
		}
		finally {
			ConstructorResolver.setCurrentInjectionPoint(previousInjectionPoint);
		}
	}
 ```
&emsp;&emsp;寻找类型的匹配执行顺序时，首先尝试使用解析器进行解析，如果解析器没有成功解析，那么可能是使用默认的解析器，或者是使用了自定义的解析器，但是对于集合等类型来说并不在解析范围之类，所以再次对不通类型进行不同类型进行不同情况的处理，虽说对不通类型处理方式不一致，但是大致的思路还是很相似的，所以函数中只对数组类型进行详细地注释。

### 7.3.3 applyPropertyValues
&emsp;&emsp;程序运行到这里，已经完成了对所有注入的属性的获取，但是获取的属性是以PropertyValues形式存在的，还没有应用到已经实例化的bean总，这一工作是在applyPropertyValues中。
 ```java
 protected void applyPropertyValues(String beanName, BeanDefinition mbd, BeanWrapper bw, PropertyValues pvs) {
		if (pvs.isEmpty()) {
			return;
		}

		if (System.getSecurityManager() != null && bw instanceof BeanWrapperImpl) {
			((BeanWrapperImpl) bw).setSecurityContext(getAccessControlContext());
		}

		MutablePropertyValues mpvs = null;
		List<PropertyValue> original;

		if (pvs instanceof MutablePropertyValues) {
			mpvs = (MutablePropertyValues) pvs;
			//如果mpvs中的值已经被转换为对应的类型那么可以直接设置到beanwapper中
			if (mpvs.isConverted()) {
				// Shortcut: use the pre-converted values as-is.
				try {
					bw.setPropertyValues(mpvs);
					return;
				}
				catch (BeansException ex) {
					throw new BeanCreationException(
							mbd.getResourceDescription(), beanName, "Error setting property values", ex);
				}
			}
			original = mpvs.getPropertyValueList();
		}
		else {
			//如果pvs并不是使用MutablePropertyValues封装的类型，那么直接使用原始的属性获取方法
			original = Arrays.asList(pvs.getPropertyValues());
		}

		TypeConverter converter = getCustomTypeConverter();
		if (converter == null) {
			converter = bw;
		}
		//获取对应的解析器
		BeanDefinitionValueResolver valueResolver = new BeanDefinitionValueResolver(this, beanName, mbd, converter);

		// Create a deep copy, resolving any references for values.
		List<PropertyValue> deepCopy = new ArrayList<>(original.size());
		boolean resolveNecessary = false;
		//便利属性，将属性转换为对应类的对应属性的类型
		for (PropertyValue pv : original) {
			if (pv.isConverted()) {
				deepCopy.add(pv);
			}
			else {
				String propertyName = pv.getName();
				Object originalValue = pv.getValue();
				if (originalValue == AutowiredPropertyMarker.INSTANCE) {
					Method writeMethod = bw.getPropertyDescriptor(propertyName).getWriteMethod();
					if (writeMethod == null) {
						throw new IllegalArgumentException("Autowire marker for property without write method: " + pv);
					}
					originalValue = new DependencyDescriptor(new MethodParameter(writeMethod, 0), true);
				}
				Object resolvedValue = valueResolver.resolveValueIfNecessary(pv, originalValue);
				Object convertedValue = resolvedValue;
				boolean convertible = bw.isWritableProperty(propertyName) &&
						!PropertyAccessorUtils.isNestedOrIndexedProperty(propertyName);
				if (convertible) {
					convertedValue = convertForProperty(resolvedValue, propertyName, bw, converter);
				}
				// Possibly store converted value in merged bean definition,
				// in order to avoid re-conversion for every created bean instance.
				if (resolvedValue == originalValue) {
					if (convertible) {
						pv.setConvertedValue(convertedValue);
					}
					deepCopy.add(pv);
				}
				else if (convertible && originalValue instanceof TypedStringValue &&
						!((TypedStringValue) originalValue).isDynamic() &&
						!(convertedValue instanceof Collection || ObjectUtils.isArray(convertedValue))) {
					pv.setConvertedValue(convertedValue);
					deepCopy.add(pv);
				}
				else {
					resolveNecessary = true;
					deepCopy.add(new PropertyValue(pv, convertedValue));
				}
			}
		}
		if (mpvs != null && !resolveNecessary) {
			mpvs.setConverted();
		}

		// Set our (possibly massaged) deep copy.
		try {
			bw.setPropertyValues(new MutablePropertyValues(deepCopy));
		}
		catch (BeansException ex) {
			throw new BeanCreationException(
					mbd.getResourceDescription(), beanName, "Error setting property values", ex);
		}
	}
 ```
## 7.4 初始化 bean
&emsp;&emsp;大家应该都记得bean配置时bean中又一个init-method的属性，这个属性的作用时在bean实例化前调用init-method指定方法来根据用户业务进行相关的实例化。我们现在已经进入这个方法了，首先看一下这个方法的执行位置，Spring中的程序已经执行过的bean的实例化，并且进行了属性的填充，而这时将会调用用户设定的初始化方法。
 ```java
protected Object initializeBean(final String beanName, final Object bean, @Nullable RootBeanDefinition mbd) {
	if (System.getSecurityManager() != null) {
		AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
			invokeAwareMethods(beanName, bean);
			return null;
		}, getAccessControlContext());
	}
	else {
		//对特殊
		invokeAwareMethods(beanName, bean);
	}

	Object wrappedBean = bean;
	if (mbd == null || !mbd.isSynthetic()) {
		//应用后置处理器
		wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
	}

	try {
		//激活用户自定义的init方法
		invokeInitMethods(beanName, wrappedBean, mbd);
	}
	catch (Throwable ex) {
		throw new BeanCreationException(
				(mbd != null ? mbd.getResourceDescription() : null),
				beanName, "Invocation of init method failed", ex);
	}
	if (mbd == null || !mbd.isSynthetic()) {
		//后处理器应用
		wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
	}

	return wrappedBean;
}
 ```
### 7.4.1 激活 Aware 方法
&emsp;&emsp;在分析其原理之前，我们先了解一下Aware的使用。Spring中提供了一些Aware相关接口，比如 BeanFactoryAware 、ApplicationContextAware 、ResourceLoaderAware 、ServletContextAware 等，实现这些Aware接口的bean在初始化之后，可以取得一些相应的资源，例如实现 BeanFactoryAware 的bean，在bean被初始化后，Spring 容器将会注入 BeanFactory 的实例，而实现 ApplicationContextAware 的 bean， 在 bean 被初始后，将会被注入ApplicationContext 的实例等。