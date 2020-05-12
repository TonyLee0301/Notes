package info.tonyle.concurrent.lock.reentrant;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 可中断
 */
public class Incorruptibly implements Runnable{

    public static ReentrantLock lock1 = new ReentrantLock();
    public static ReentrantLock lock2 = new ReentrantLock();

    private int lock;

    public Incorruptibly(int lock){
        this.lock = lock;
    }

    public void run() {
        try{
            if(lock == 1){
                lock1.lockInterruptibly();
                try{
                    Thread.sleep(500);
                }catch (Exception e){}
                lock2.lockInterruptibly();
            }else{
                if(lock == 2){
                    lock2.lockInterruptibly();
                    try{
                        Thread.sleep(500);
                    }catch (Exception e){}
                    lock1.lockInterruptibly();
                }
            }
        }catch (Exception e){
        }finally {
            if(lock1.isHeldByCurrentThread()){
                lock1.unlock();
            }
            if(lock2.isHeldByCurrentThread()){
                lock2.unlock();
            }
            System.out.println(Thread.currentThread().getId() + "线程退出");
        }
    }

    static class DeadLockChecker{
        private final static ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
        final static Runnable deadlockChecker = new Runnable() {
            public void run() {
                while(true){
                    long[] deadlockThreadIds = mbean.findMonitorDeadlockedThreads();
                    if(deadlockThreadIds != null){
                        ThreadInfo[] threadInfos = mbean.getThreadInfo(deadlockThreadIds);
                        for(Thread t : Thread.getAllStackTraces().keySet()){
                            for(int i = 0; i < threadInfos.length; i++){
                                if(t.getId() == threadInfos[i].getThreadId()){
                                    t.interrupt();
                                }
                            }
                        }
                    }
                    try{
                        Thread.sleep(5000);
                    }catch (Exception e){}
                }
            }
        };

        public static void check()
        {
            Thread t = new Thread(deadlockChecker);
            t.setDaemon(true);
            t.start();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Incorruptibly incorruptibly1 = new Incorruptibly(1);
        Incorruptibly incorruptibly2 = new Incorruptibly(2);
        Thread thread1 = new Thread(incorruptibly1);
        Thread thread2 = new Thread(incorruptibly2);
        thread1.start();
        thread2.start();
        Thread.sleep(1000);
        DeadLockChecker.check();
    }
}
