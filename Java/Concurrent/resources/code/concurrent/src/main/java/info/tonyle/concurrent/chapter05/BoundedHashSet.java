package info.tonyle.concurrent.chapter05;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;

/**
 * 信号量用来控制同时访问某个特定资源的操作数量，或者同时执行某个操作的数量[CPJ 3.4.1]
 * @param <T>
 */
public class BoundedHashSet<T> {

    private final Set<T> set;

    private final Semaphore sem;

    public BoundedHashSet(int bound){
        this.set = Collections.synchronizedSet(new HashSet<T>(bound));
        this.sem = new Semaphore(bound);
    }

    public boolean add(T o) throws InterruptedException {
        sem.acquire();
        boolean wasAdded = false;
        try{
            wasAdded = set.add(o);
            return wasAdded;
        }finally {
            if(!wasAdded){
                sem.release();
            }
        }
    }

    public boolean remove(T o){
        boolean wasRemove = set.remove(o);
        if(wasRemove){
            sem.release();
        }
        return wasRemove;
    }

    @Override
    public String toString(){
        return this.set.toString() + "\n" +this.sem.getQueueLength();
    }

    public static void main(String[] args) throws InterruptedException {
        BoundedHashSet boundedHashSet = new BoundedHashSet<String>(10);
        new Thread(()->{
            for(int i = 0; i < 20; i++){
                try {
                    System.out.println("添加："+i + " " + boundedHashSet.add(i+""));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println(boundedHashSet.toString());
        }).start();
        Thread.sleep(1000 * 3);
        new Thread(()->{
            for(int i = 0; i < 10; i++){
                System.out.println("移除："+i + " " + boundedHashSet.remove(i+""));
            }
        }).start();
    }
}
