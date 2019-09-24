package info.tonylee.jvm.gc;

/**
 * @ClassName ReferenceCountingGC
 * @Description 对象间循环引用，引用计数算法缺陷
 *
 * testGC()方法执行后，objA 和 objB会不会GC呢？
 *
 * @Author tonylee
 * @Date 2019-09-12 17:33
 * @Version 1.0
 **/
public class ReferenceCountingGC {

    public Object instance = null;

    private static int _1MB = 1024 * 1024;

    /**
     * 这个成员变量的意义就是占用内存，以便在GC日志中看清楚是否被回收
     */
    private byte[] bigSize = new byte[2*_1MB];

    public static void testGC(){
        ReferenceCountingGC objA = new ReferenceCountingGC();
        ReferenceCountingGC objB = new ReferenceCountingGC();

        objA.instance = objB;
        objB.instance = objA;

        objA = null;
        objB = null;

        //假设这行发生了GC，objA 和 objB 是否能被回收？
        System.gc();

    }

    public static void main(String[] args) {
        testGC();
    }

}
