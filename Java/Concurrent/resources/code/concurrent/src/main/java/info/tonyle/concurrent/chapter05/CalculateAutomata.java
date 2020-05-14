package info.tonyle.concurrent.chapter05;

import javafx.concurrent.Worker;

import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * 栅栏用于控制所有线程到达栅栏位置才可同时执行
 */
public class CalculateAutomata {

    private final CyclicBarrier barrier;

    private Runnable[] workers;

    public CalculateAutomata(){
        int count = Runtime.getRuntime().availableProcessors();
        barrier = new CyclicBarrier(count, ()->{
            System.out.println(Thread.currentThread().getId() + " work is done cyclicBarrier to do the otherThings.");
        });
        workers = new Runnable[count];
        Random random = new Random();
        for(int i = 0; i < count; i ++){
            final int index = i;
            workers[i] = () -> {
                try {
                    System.out.println(Thread.currentThread().getId() + " is work");
                    Thread.sleep(random.nextInt(5) * 1000);
                    barrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }
            };
        }
    }

    public void start() throws BrokenBarrierException, InterruptedException {
        for(int i = 0; i < workers.length; i++){
            new Thread(workers[i]).start();
        }
    }

    public static void main(String[] args) throws BrokenBarrierException, InterruptedException {
        new CalculateAutomata().start();
    }

}
