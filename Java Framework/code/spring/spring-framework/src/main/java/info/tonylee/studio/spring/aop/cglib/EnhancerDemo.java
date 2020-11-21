package info.tonylee.studio.spring.aop.cglib;


import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

public class EnhancerDemo {

    public static void main(String[] args) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(EnhancerDemo.class);
        enhancer.setCallback(new MethodInterceptorImpl());

        EnhancerDemo demo = (EnhancerDemo) enhancer.create();
        demo.test();
        System.out.println("-----------------");
        demo.test1();
//        System.out.println(demo);
    }

    public void test(){
        System.out.println("EnhancerDemo test()");
        test1();
    }

    public void test1(){
        System.out.println("test1()");
    }

    private static class MethodInterceptorImpl implements MethodInterceptor {

        public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
            if("test".equals(method.getName())){
                System.err.println("Before invoke " + method);
                Object result = methodProxy.invokeSuper(o, objects);
                System.err.println("After invoke " + method);
                return result;
            }
            if("test1".equals(method.getName())){
                System.out.println("Before invoke " + method);
                Object result = methodProxy.invokeSuper(o, objects);
                System.out.println("After invoke " + method);
                return result;
            }
            return method.invoke(o,objects);
        }
    }
}
