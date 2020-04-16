package info.tonyle.concurrent.chapter04;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 失效的委托
 * 两个线程安全的变量组成，但其操作包含了无效的状态转换。
 */
public class DelegateInvalid {
    //不变条件 lower < upper
    private final AtomicInteger lower = new AtomicInteger(0);
    private final AtomicInteger upper= new AtomicInteger(0);

    public void setLower(Integer i){
        //不安全的复合操作，检查 设置
        if(i > upper.get()){
            throw new IllegalArgumentException("can not set lower > upper");
        }
        lower.set(i);
    }

    public void setUpper(Integer i){
        //不安全的复合操作，检查 设置
        if(i < lower.get()){
            throw new IllegalArgumentException("can not set upper < lower");
        }
        upper.set(i);
    }

}
