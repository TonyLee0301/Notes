package info.tonyle.concurrent.chapter05.readExample;

import java.util.List;
import java.util.concurrent.*;

public class CompletionServiceReader extends Reader {
    private final Executor executor;
    public CompletionServiceReader(Executor executor){
        this.executor = executor;
    }
    void readerPage(CharSequence source){
        List<ImageInfo> infos = scanForImageInfo(source);
        CompletionService<ImageData> completionService = new ExecutorCompletionService<>(executor);
        for(final ImageInfo imageInfo : infos){
            completionService.submit(()->imageInfo.downLoadImage());
        }
        readText(source);
        try {
            for(int i = 0; i < infos.size(); i++){
                Future<ImageData> future = completionService.take();
                ImageData imageData = future.get();
                readImage(imageData);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }
}
