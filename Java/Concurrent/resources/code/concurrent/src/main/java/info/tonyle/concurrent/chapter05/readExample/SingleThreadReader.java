package info.tonyle.concurrent.chapter05.readExample;

public class SingleThreadReader extends Reader{
    void readerPage(CharSequence source){
        readText(source);
        for(ImageInfo imageInfo : scanForImageInfo(source)){
            readImage(imageInfo.downLoadImage());
        }
    }
}
