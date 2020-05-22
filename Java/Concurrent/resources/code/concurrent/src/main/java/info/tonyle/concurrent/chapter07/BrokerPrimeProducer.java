package info.tonyle.concurrent.chapter07;

import java.math.BigInteger;
import java.util.concurrent.BlockingQueue;

/**
 * 不要这么做,当生产者在BlockingQueue.put方法阻塞时，无法检查到关闭标志，因此会一直阻塞。
 */
public class BrokerPrimeProducer extends Thread{

    private final BlockingQueue<BigInteger> queue;

    private volatile boolean cancelled = false;

    public BrokerPrimeProducer(BlockingQueue<BigInteger> queue){
        this.queue = queue;
    }

    public void cancel(){
        cancelled = true;
    }

    @Override
    public void run(){
        BigInteger p = BigInteger.ONE;
        try {
            while(!cancelled){
                queue.put(p = p.nextProbablePrime());
            }
        } catch (InterruptedException e) {
        }
    }
}
