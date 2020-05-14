package info.tonyle.concurrent.chapter05.readExample;

public class SingleThreadReader {
    private Reader reader;
    void readerPage(CharSequence source){
        reader.readText(source);
        for(ImageInfo imageInfo : reader.scanForImageInfo(source)){
            reader.readImage(imageInfo.downLoadImage());
        }
    }
}
