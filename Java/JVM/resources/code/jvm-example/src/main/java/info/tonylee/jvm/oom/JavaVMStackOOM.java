package info.tonylee.jvm.oom;

/**
 * @ClassName JavaVMStackOOM
 * @Description 测试 虚拟机栈 申请内存 出现内存溢出的情况
 * @Author tonylee
 * @Date 2019-09-11 22:53
 * @Version 1.0
 *
 * 注意，请不要在windows mac 上测试，在 linux 还未实验。。
 *
 * VM Args : -Xss2M （不妨设置大点）
 *
 **/
public class JavaVMStackOOM {

    private void dontStop(){
        while(true){}
    }

    public void stackLeakByThread(){
        long i = 0;
        while(true){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    dontStop();
                }
            }).start();
            System.out.println("当前创建线程数： " + (++i));
        }
    }

    public static void main(String[] args) {
        JavaVMStackOOM javaVMStackOOM = new JavaVMStackOOM();
        javaVMStackOOM.stackLeakByThread();
    }
}
