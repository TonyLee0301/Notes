package info.tonylee.jvm.gc;

/**
 * @ClassName FinalizeEscapeGC
 * @Description 此代码演示两点：
 *                  1.对象可以在被GC时自我拯救。
 *                  2.这种自救的机会只有一次，因为一个对象的finalize()方法最多只会被系统调用一次
 * @Author tonylee
 * @Date 2019-09-18 18:43
 * @Version 1.0
 **/
public class FinalizeEscapeGC {

    public static FinalizeEscapeGC SAVE_HOOK = null;

    public void isAlive(){
        System.out.println("yes,i'am still alive:)");
    }

    @Override
    protected void finalize() throws Throwable{
        super.finalize();
        System.out.println("finalize method executed!");
        FinalizeEscapeGC.SAVE_HOOK = this;
    }

    public static void main(String[] args) throws Throwable{
        SAVE_HOOK = new FinalizeEscapeGC();
        //对象第一次拯救自己
        saveOneself();
        //对象再次自救，但失败
        saveOneself();
    }

    private static void saveOneself() throws Throwable{
        //对象拯救自己
        SAVE_HOOK = null;
        System.gc();
        //因为finalize方法优先级很低，所以暂停0.5秒以等待它。
        Thread.sleep(5 * 100);
        if(SAVE_HOOK != null){
            SAVE_HOOK.isAlive();
        }else{
            System.out.println("no,i'am dead:(");
        }
    }

}
