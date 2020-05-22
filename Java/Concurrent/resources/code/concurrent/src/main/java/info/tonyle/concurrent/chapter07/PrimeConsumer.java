package info.tonyle.concurrent.chapter07;

import java.math.BigInteger;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class PrimeConsumer extends Thread{

    private static volatile boolean needPrime = true;

    @Override
    public void run(){
        try {
            consumerPrimes();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    void consumerPrimes() throws InterruptedException{
        BlockingQueue<BigInteger> blockingQueue = new ArrayBlockingQueue<>(10);
        //使用自定义中断标志，导致无法中断线程
        BrokerPrimeProducer producer = new BrokerPrimeProducer(blockingQueue);
//        PrimeProducer producer = new PrimeProducer(blockingQueue);
        producer.start();
        try{
            while(needPrime){
                blockingQueue.take();
                Thread.sleep(100);
            }
        }finally {
            producer.cancel();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        PrimeConsumer primeConsumer = new PrimeConsumer();
        primeConsumer.start();
        Thread.sleep(1000);
        needPrime = false;
        System.out.println(needPrime);
    }

}
