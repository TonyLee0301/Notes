package info.tonylee.studio.spring.eventListner;

import org.springframework.context.ApplicationEvent;

public class TestEvent extends ApplicationEvent {

    private String msg;

    public TestEvent(Object source) {
        super(source);
    }

    public TestEvent(Object source, String msg){
        super(source);
        this.msg = msg;
    }

    public void print(){
        System.out.println(msg);
    }
}
