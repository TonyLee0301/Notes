package info.tonyle.concurrent.chapter08;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;

public class BoundedExecutor {
    private final Executor executor;

    private final Semaphore semaphore;

    public BoundedExecutor(Executor executor, int bound){
        this.executor = executor;
        this.semaphore = new Semaphore(bound);
    }

    public void submitTask(final Runnable runnable) throws InterruptedException {
        semaphore.acquire();
        try{
            executor.execute(() -> {
                try{
                    runnable.run();
                }finally {
                    semaphore.release();
                }
            });
        }catch (RejectedExecutionException e){
            semaphore.release();
        }
    }
}
