package info.tonyle.concurrent.chapter05.readExample;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class FutureReader extends Reader {
    private final ExecutorService executor = Executors.newCachedThreadPool();

    void readerPage(CharSequence source){
        final List<ImageInfo> imageInfos = scanForImageInfo(source);
        Callable<List<ImageData>> task =
                ()-> imageInfos.stream().map(i -> i.downLoadImage()).collect(Collectors.toList());
        Future<List<ImageData>> future = executor.submit(task);
        readText(source);
        List<ImageData> imageDataList = null;
        try {
            imageDataList = future.get();
            for(ImageData imageData : imageDataList){
                readImage(imageData);
            }
        } catch (InterruptedException e) {
            //重新设置线程的中断状态
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }
}
