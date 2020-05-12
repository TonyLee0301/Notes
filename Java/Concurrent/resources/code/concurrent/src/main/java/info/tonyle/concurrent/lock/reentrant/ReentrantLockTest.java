package info.tonyle.concurrent.lock.reentrant;

import java.util.concurrent.locks.ReentrantLock;

/**
 * ReentrantLockTest 重入锁
 * l
 */
public class ReentrantLockTest implements  Runnable{

    public static ReentrantLock lock = new ReentrantLock();
    public static int i = 0;

    public void run() {
        for(int j = 0; j < 100000; j++){
            lock.lock();
            try{
                i++;
            }finally {
                lock.unlock();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException
    {
        ReentrantLockTest test = new ReentrantLockTest();
        Thread t1 = new Thread(test);
        Thread t2 = new Thread(test);
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println(i);
    }
}
