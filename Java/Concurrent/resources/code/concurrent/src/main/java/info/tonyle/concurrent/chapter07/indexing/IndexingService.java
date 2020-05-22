package info.tonyle.concurrent.chapter07.indexing;

import java.io.File;
import java.util.concurrent.BlockingQueue;

public class IndexingService {
    private static final File POISON = new File("");
    private final BlockingQueue<File> queue;
    private final File root;

    public IndexingService(File root, BlockingQueue<File> queue){
        this.root = root;
        this.queue = queue;
    }

    private CrawlerThread producer = new CrawlerThread();
    private IndexerThread consumer = new IndexerThread();

    public void start(){
        producer.start();
        consumer.start();
    }

    public void stop(){
        producer.interrupt();
    }

    public void awaitTermination() throws InterruptedException{
        consumer.join();
    }

    class CrawlerThread extends Thread {
        public void run() {
            try {
                crawl(root);
            } catch (InterruptedException e) {
            } finally {
                while (true) {
                    try {
                        queue.put(POISON);
                    }catch (InterruptedException e){
                        //重试
                    }
                }
            }
        }
        private void crawl(File root) throws InterruptedException {
        }
    }
    class IndexerThread extends Thread {
        @Override
        public void run(){
            try{
                while(true){
                    File file = queue.take();
                    if(file == POISON){
                        break;
                    }else{
                        indexFile(file);
                    }
                }
            } catch (InterruptedException e) {
            }
        }
        private void indexFile(File file){}
    }
}
