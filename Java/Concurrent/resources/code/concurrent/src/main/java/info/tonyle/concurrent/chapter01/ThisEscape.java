package info.tonyle.concurrent.chapter01;

/**
 * this 逸出
 * 在构造过程中使this应用逸出的一个常见错误是，在构造函数中启动一个线程。
 * 当构造函数
 *
 */
public class ThisEscape {
    private int a = 0;
    public ThisEscape(){
        new Thread(()->{
            System.out.println(a);
        }).start();
        try {
            Thread.sleep(3);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        a = 15;
    }

    public static void main(String[] args) {
        new ThisEscape();
    }
}
