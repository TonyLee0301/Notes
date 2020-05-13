package info.tonyle.concurrent.chapter05.resultcache;

public interface Computable<A,V> {
    V compute(A arg) throws InterruptedException;
}
