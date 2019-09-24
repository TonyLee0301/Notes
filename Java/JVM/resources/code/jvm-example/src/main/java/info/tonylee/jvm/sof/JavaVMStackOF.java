package info.tonylee.jvm.sof;

/**
 * @ClassName JavaVMStackOF
 * @Description 测试虚拟机栈溢出
 * @Author tonylee
 * @Date 2019-09-11 22:29
 * @Version 1.0
 *
 * VM Args: -Xss160K (JDK 1.8 后 设置了最小栈空间大小 为160)
 *
 **/
public class JavaVMStackOF {

    private int stackLength = 1;

    public void stackLeak(){
        stackLength++ ;
        stackLeak();
    }

    public static void main(String[] args) {

        JavaVMStackOF javaVMStackOF = new JavaVMStackOF();

        try {
            javaVMStackOF.stackLeak();
        }catch (Throwable e){
            System.out.println("stack length: " + javaVMStackOF.stackLength);
            throw e;
        }

    }

}
