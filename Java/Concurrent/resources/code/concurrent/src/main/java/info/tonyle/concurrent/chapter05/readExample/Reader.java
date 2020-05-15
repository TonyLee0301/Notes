package info.tonyle.concurrent.chapter05.readExample;

import java.util.Collections;
import java.util.List;

public class Reader {

    void readText(CharSequence source){}

    void readImage(ImageData imageData){}

    List<ImageInfo> scanForImageInfo(CharSequence source){
        return Collections.EMPTY_LIST;
    }

    static class ImageInfo{
        public ImageData downLoadImage(){
            return new ImageData();
        }
    }
    static class ImageData{
    }
}
