package info.tonyle.concurrent.chapter07;

import java.io.PrintWriter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * 不支持关闭的生产者-消费者日志服务
 */
public class LogWriter {

    private final BlockingQueue queue;
    private final LoggerThread logger;

    public LogWriter(PrintWriter writer){
        this.queue = new LinkedBlockingDeque(100);
        logger = new LoggerThread(writer);
    }

    public void start(){
        logger.start();
    }

    public void log(String msg) throws InterruptedException{
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
                    writer.println(queue.take());
                }
            } catch (InterruptedException e) {
            }finally {
                writer.close();
            }
        }
    }
}
