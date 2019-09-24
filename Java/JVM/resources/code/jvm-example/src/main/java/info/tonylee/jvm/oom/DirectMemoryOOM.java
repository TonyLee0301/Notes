package info.tonylee.jvm.oom;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * @ClassName DirectMemoryOOM
 * @Description 使用 unsafe 分配内存
 * @Author tonylee
 * @Date 2019-09-12 00:58
 * @Version 1.0
 *
 * VM args : -Xmx20M -XX:MaxDirectMemorySize=10M
 **/
public class DirectMemoryOOM {

    private static final int _1MB = 1024*1024;

    public static void main(String[] args) throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredFields()[0];
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe)unsafeField.get(null);
        while(true){
            unsafe.allocateMemory(_1MB);
        }
    }
}
