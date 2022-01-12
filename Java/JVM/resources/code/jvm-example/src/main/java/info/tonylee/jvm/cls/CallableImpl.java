package info.tonylee.jvm.cls;

/**
 * @Author：Tony Lee
 * @Date：2022/1/4 2:24 下午
 */
public class CallableImpl implements Callable{

    @Override
    public void call(Object obj) {
        System.out.println(obj.toString());
    }
}
