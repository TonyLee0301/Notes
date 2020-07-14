package info.tonyle.concurrent.test;

import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Test {

    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(5,20,200, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(200),new MyThreadFactory("test-executor"), new ThreadPoolExecutor.CallerRunsPolicy());

    static class MyThreadFactory implements ThreadFactory {

        AtomicInteger atomicInteger = new AtomicInteger(1);

        private String poolName;

        public MyThreadFactory(String poolName){
            this.poolName = poolName;
        }

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, poolName + "-" + atomicInteger.getAndIncrement());
        }
    }

    static class Task extends Thread{
        String id;
        public Task(String id){
            this.id = id;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(Thread.currentThread().getName() +"执行:   "+ id);
        }
    }

    public static void main(String[] args) {
        Long start = System.currentTimeMillis();
        AtomicLong atomicLong = new AtomicLong(0);
        Thread thread = new Thread(()->{
            Long id;
            while(( id = atomicLong.incrementAndGet()) < 1000){
                Task task = new Task(id + "");
                executor.execute(task);
//                System.out.println("当前队列："+executor.getQueue().size() + "，已执行玩别的任务数目："+executor.getCompletedTaskCount());
            }
            executor.shutdown();
            System.out.println("结束："+ (System.currentTimeMillis() - start)/1000);
        });
        thread.start();
    }

}
