package info.tonyle.concurrent.chapter07;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class InterruptThread {

    private static final BlockingQueue<String> queue = new ArrayBlockingQueue<>(5);

    public static void main(String[] args) throws InterruptedException {
        Thread thread = new Thread(()->{
            try{
                System.out.println("开启线程");
                while(true){
                    try{
                        System.out.println("线程正在执行");
                        queue.take();
                        Thread.sleep(1 * 1000);
                    } catch (InterruptedException e) {
                        System.out.println("发现中断");
//                        System.out.println("尝试停止");
//                        Thread.currentThread().interrupt();
//                        System.out.println(Thread.currentThread());
                        System.out.println("catch   " + Thread.currentThread().interrupted());
//                        throw e;
                    }finally {
                        if(Thread.currentThread().isInterrupted()){
                            System.out.println("执行 Thread.currentThread().interrupt() 尝试恢复");
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
            /*catch (InterruptedException e){
                if(Thread.currentThread().isInterrupted()){
                    System.out.println("执行 Thread.currentThread().interrupt() 尝试恢复");
                    Thread.currentThread().interrupt();
                }
            }*/
            finally {

            }
        });
        thread.start();

        Thread.sleep(1000);

        System.out.println("执行中断");
        thread.interrupt();
        System.out.println(thread);
        System.out.println(thread.isInterrupted());
    }

}
