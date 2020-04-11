package info.tonyle.concurrent.chapter01;

import info.tonyle.concurrent.UnThreadSafe;

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
    }
}
