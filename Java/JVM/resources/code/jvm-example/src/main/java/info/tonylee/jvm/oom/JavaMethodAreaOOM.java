package info.tonylee.jvm.oom;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * @ClassName JavaMethodAreaOOM
 * @Description 借助 CGLib使方法区出现内存溢出异常
 * @Author tonylee
 * @Date 2019-09-12 00:42
 * @Version 1.0
 *
 * VM Args : -XX:PermSize=10M -XX:MaxPermSize=10M
 *
 *
 **/
public class JavaMethodAreaOOM {

    static class OOMObject{}

    public static void main(String[] args) {
        while(true){
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(OOMObject.class);
            enhancer.setUseCache(false);
            enhancer.setCallback(new MethodInterceptor() {
                @Override
                public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
                    return proxy.invoke(obj,args);
                }
            });
            enhancer.create();
        }
    }

}
