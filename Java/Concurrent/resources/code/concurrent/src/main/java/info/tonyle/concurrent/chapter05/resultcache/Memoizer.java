package info.tonyle.concurrent.chapter05.resultcache;

import java.util.Map;
import java.util.concurrent.*;

public class Memoizer<A, V> implements Computable<A, V> {

    //ConcurrentHashMap 代替 HashMap
//    private final Map<A, V> cache = new ConcurrentHashMap<>();
    //使用 Future 来避免两个线程通过计算重复执行。如果一个线程正在执行a计算，
    // 另外一个线程进来执行a计算，那么通过future.get即可马上获取到对应的值
    private final Map<A, Future<V>> cache = new ConcurrentHashMap<>();

    private final Computable<A, V> c;

    public Memoizer(Computable<A, V> computable) {
        this.c = computable;
    }

    //确保不会有两个线程同时访问修改HashMap，使用ConcurrentHashMap
    @Override
    public V compute(A arg) throws InterruptedException {
        // Concurrent<A,V> 版本，无法解决同时重复计算的问题
       /* V result = cache.get(arg);
        if(result == null){
            result = c.compute(arg);
            cache.put(arg, result);
        }
        return result;
        */
      /* Future<V> future = cache.get(arg);
       if(future == null){
           Callable<V> eval = () -> c.compute(arg);
           FutureTask<V> ft = new FutureTask<>(eval);
           //可能出现两个线程同时执行该方法，那么future会被覆盖
           future = ft;
           cache.put(arg, future);
           ft.run();
       }*/
        while (true) {
            Future<V> future = cache.get(arg);
            if (future == null) {
                Callable<V> eval = () -> c.compute(arg);
                FutureTask<V> ft = new FutureTask<>(eval);
                //添加成功返回null，添加失败，返回原值。
                future = cache.putIfAbsent(arg, ft);
                //第一次添加成功肯定会失败
                //所以加个while循环。获取future
                if (future == null) {
                    future = ft;
                    ft.run();
                }
            }
            try {
                return future.get();
            }catch(CancellationException e){
                cache.remove(arg,future);
            } catch (ExecutionException e) {
                throw new InterruptedException(e.getMessage());
            }
        }
    }
}
