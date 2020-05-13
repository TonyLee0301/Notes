package info.tonyle.concurrent.chapter05.resultcache;

import java.math.BigInteger;
import java.util.Random;

public class ExpensiveFunction implements Computable<String, BigInteger> {
    private final Random random = new Random();
    @Override
    public BigInteger compute(String arg) throws InterruptedException {
        Thread.sleep(random.nextInt(10) * 1000);
        return new BigInteger(arg);
    }
}
