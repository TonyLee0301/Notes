package info.tonylee.studio.spring.eventListner;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.ResolvableType;

public class TestEventListener implements ApplicationListener {
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if(applicationEvent instanceof TestEvent){
            ((TestEvent)applicationEvent).print();
        }
    }

    public static void main(String[] args) {
        A a = new A1();
        ResolvableType resolvableType = ResolvableType.forClass(a.getClass());
        System.out.println(resolvableType);
        ResolvableType r1 = resolvableType.as(A.class);
        System.out.println(r1);
        System.out.println(r1.getGeneric());

//        ApplicationContext context = new ClassPathXmlApplicationContext("META-INF/test-event-listener.xml");
//        context.publishEvent(new TestEvent("hello", "msg"));
    }
}


interface A<T>{

}
class A1 implements A<B>{

}
class B{

}