package info.tonylee.jvm.oom;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName HeapOOM
 * @Description 测试 堆内存溢出
 * @Author tonylee
 * @Date 2019-09-11 17:45
 * @Version 1.0
 *
 *
 * VM Args：-Xms20m-Xmx20m -XX:+HeapDumpOnOutOfMemoryError
 *
 *
 **/
public class HeapOOM {

    static class OOMObject{}

    public static void main(String[] args) {

        List<OOMObject> list = new ArrayList<>();
        while(true){
            list.add(new OOMObject());
        }

    }

}
