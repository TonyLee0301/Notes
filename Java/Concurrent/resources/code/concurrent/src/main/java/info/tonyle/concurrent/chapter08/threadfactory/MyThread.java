package info.tonyle.concurrent.chapter08.threadfactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MyThread extends Thread {
    private final static String DEFAULT_NAME = "MyThreadName";
    private static volatile boolean debugLifecycle = false;
    private static final AtomicInteger alive = new AtomicInteger();
    private static final AtomicInteger created = new AtomicInteger();
    private static Logger logger = Logger.getAnonymousLogger();

    public MyThread(Runnable runnable){
        this(runnable,DEFAULT_NAME);
    }

    public MyThread(Runnable runnable, String poolName){
        super(runnable, poolName + "-" + created.incrementAndGet());
        setUncaughtExceptionHandler(
                new Thread.UncaughtExceptionHandler(){
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        logger.log(Level.SEVERE,"Uncaught in thread " + t.getName(), e);
                    }
                }
        );
    }

    @Override
    public void run(){
        //复制debug标志以确保唯一性
        boolean debug = debugLifecycle;
        if(debug) logger.log(Level.FINE,"Created" + getName());
        try{
            alive.incrementAndGet();
            super.run();
        }finally {
            alive.decrementAndGet();
            if(debug) logger.log(Level.FINE,"Exiting" + getName());
        }
    }
}
