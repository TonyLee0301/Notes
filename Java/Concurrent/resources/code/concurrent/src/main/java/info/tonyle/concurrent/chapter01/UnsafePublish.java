package info.tonyle.concurrent.chapter01;

import info.tonyle.concurrent.UnThreadSafe;

import java.util.Collections;

@UnThreadSafe
public class UnsafePublish {

    private String[] states = {"a","b","c"};

    /**
     * 通过public级别修饰的方法，变相的将类的私域发布到外部，任何外部线程都可以访问、修改该域。
     * 这样是不安全的，因为我们无法检查其他线程是否会修改这个域导致错误。
     * @return
     */
    public String[] getStates(){
        return states;
    }

    public static void main(String[] args) {
        UnsafePublish unsafePublish = new UnsafePublish();
        unsafePublish.getStates()[0] = "d";
        new Thread(()->{
            unsafePublish.initialize();
            unsafePublish.holder.assertSanity();
        }).start();
        new Thread(()->{
            unsafePublish.holder.assertSanity();
        }).start();
    }

    /** 不安全的发布 **/
    public Holder holder;

    public void initialize(){
        holder = new Holder(42);
    }
}


class Holder{
    private int n;
    public Holder(int n){
        this.n = n;
    }

    /***
     * 由于Holder的不安全发布，可能存在多个线程访问Holder
     */
    public void assertSanity(){
        if(n != n){
            throw new AssertionError("This statement is false.");
        }
    }
}
