package info.tonyle.concurrent.chapter07;

import java.io.PrintWriter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class LoggerService {
    private final BlockingQueue<String> queue;
    private final LoggerThread loggerThread;
    private boolean isShutdown;
    private int reservations;
    public LoggerService(PrintWriter writer){
        queue = new LinkedBlockingDeque(100);
        loggerThread = new LoggerThread(writer);
    }

    public void start(){
        loggerThread.start();
    }

    public void stop(){
        synchronized (this){
            isShutdown = true;
        }
        loggerThread.interrupt();
    }

    public void log(String msg) throws InterruptedException{
        synchronized (this){
            if(isShutdown){
                throw new IllegalStateException("logger is shutdown");
            }
            reservations++;
        }
        queue.put(msg);
    }

    private class LoggerThread extends Thread{
        private final PrintWriter writer;
        public LoggerThread(PrintWriter writer){
            this.writer = writer;
        }

        @Override
        public void run(){
            try{
                while(true){
                    try{
                        synchronized (LoggerService.this) {
                            if(isShutdown && reservations == 0) {
                                break;
                            }
                        }
                        String msg = queue.take();
                        synchronized (LoggerService.this){
                            reservations--;
                        }
                        writer.println(msg);
                    }catch (InterruptedException e){};
                }
            } finally {
                writer.close();
            }
        }
    }
}
