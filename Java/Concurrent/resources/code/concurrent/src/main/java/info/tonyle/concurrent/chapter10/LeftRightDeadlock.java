package info.tonyle.concurrent.chapter10;

public class LeftRightDeadlock {
    private final Object left = new Object();
    private final Object right = new Object();

    public void leftRight(){
        synchronized (left) {
            synchronized (right){
                doSomething();
            }
        }
    }

    public void rightLeft(){
        synchronized (right){
            synchronized (left){
                doSomething();
            }
        }
    }

    public void doSomething(){
        System.out.println("is_ok");
    }

    public static void main(String[] args) {
        final LeftRightDeadlock leftRightDeadlock = new LeftRightDeadlock();
        new Thread(()->{
            while(true) leftRightDeadlock.leftRight();
        }).start();
        new Thread(()->{
            while(true) leftRightDeadlock.rightLeft();
        }).start();
    }
}
