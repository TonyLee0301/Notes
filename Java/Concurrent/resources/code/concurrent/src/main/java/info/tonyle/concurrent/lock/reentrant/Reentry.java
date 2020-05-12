package info.tonyle.concurrent.lock.reentrant;

/**
 * reentry 重入
 * 同一个线程针对该锁，能反复进入
 * lock.lock();
 * lock.lock();
 * lock.unlock();
 * lock.unlock();
 * synchronized 就是重入锁
 */
public class Reentry {
    final static Child child = new Reentry().new Child();//为了保证锁唯一
    class Child extends Father implements Runnable{
        @Override
        public synchronized void doSomeThing(){
            doAnotherThing();
            System.out.println("child do something");
        }

        private synchronized void doAnotherThing(){
            super.doSomeThing();
            System.out.println("child do anotherThing");
        }

        public void run() {
            this.doSomeThing();
        }
    }

    class Father{
        public synchronized void doSomeThing(){
            System.out.println("father do something()");
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < 50; i++) {
            new Thread(child).start();
        }
    }
}
