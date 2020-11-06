package info.tonylee.spring.test;


import java.lang.ref.WeakReference;
import java.lang.reflect.Field;

public class TestThreadLocal {
    public static void main(String[] args) throws Exception {
        Thread thread = new Thread(()->{
            ThreadLocal threadLocal = new ThreadLocal();
            threadLocal.set(new byte[2048]);
            try {
                Thread.sleep( 1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        thread.start();
        Thread.sleep(500L);
        Field field = thread.getClass().getDeclaredField("threadLocals");
        field.setAccessible(true);
        Object object = field.get(thread);
        Thread.sleep(1000L);
        Field table = object.getClass().getDeclaredField("table");
        table.setAccessible(true);
        Object t = table.get(object);
        object = null;
        System.gc();
        if(t.getClass().isArray()){
            Object[] list = (Object[])t;
            System.out.println(list.length);
            for(Object entry : list){
                 if(entry != null){
                     System.out.println(entry.getClass());
                     if(entry instanceof WeakReference){
                         System.out.println("key  "+  ((WeakReference)entry).get());
                     }
                     Field value = entry.getClass().getDeclaredField("value");
                     value.setAccessible(true);
                     object = value.get(entry);
                     System.out.println(object);

                 }

            }
        }
        t = null;
        System.gc();
        System.out.println(object);
    }

}
