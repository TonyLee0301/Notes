package info.tonyle.concurrent.chapter05;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

/**
 * 闭锁
 */
public class TestHarness {

    public long timeTasks(int nThreads, final Runnable task) throws InterruptedException {
        final CountDownLatch startGate = new CountDownLatch(1);
        final CountDownLatch endGate = new CountDownLatch(nThreads);

        for(int i = 0; i < nThreads; i++){
            Thread t = new Thread(() -> {
                try{
                    startGate.await();
                    task.run();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    endGate.countDown();
                }
            });
            t.start();
        }
        long starTime = System.nanoTime();
        startGate.countDown();
        endGate.await();
        long endTime = System.nanoTime();
        return endTime - starTime;

    }

    public static void main(String[] args) throws InterruptedException {
        TestHarness testHarness = new TestHarness();
        final Random random = new Random();
        long time = testHarness.timeTasks(5,()->{
            try {
                Thread.sleep(random.nextInt(5) * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(Thread.currentThread().getName());
        });
        System.out.println(time);
    }

}
