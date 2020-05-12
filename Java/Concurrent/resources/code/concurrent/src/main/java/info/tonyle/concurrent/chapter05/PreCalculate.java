package info.tonyle.concurrent.chapter05;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class PreCalculate {

    private final FutureTask<Long> futureTask = new FutureTask<Long>(new Callable<Long>() {
        @Override
        public Long call() throws Exception {
            Thread.sleep(3 * 1000);
            return new Random().nextLong();
        }
    });

    private final Thread thread = new Thread(futureTask);

    public void start(){thread.start();}

    public Long get() throws ExecutionException, InterruptedException {
        return futureTask.get();
    }

    public static void main(String[] args) throws Exception{
        PreCalculate preCalculate = new PreCalculate();
        preCalculate.start();
        long start = System.currentTimeMillis();
        System.out.println(preCalculate.get());
        long end = System.currentTimeMillis();
        System.out.println(end - start);
    }

}
