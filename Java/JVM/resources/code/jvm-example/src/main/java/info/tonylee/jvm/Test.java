package info.tonylee.jvm;

/**
 * @ClassName Test
 * @Description TODO
 * @Author tonylee
 * @Date 2020-11-24 20:56
 * @Version 1.0
 **/
public class Test {

    public static void main(String[] args) {
        Float bj = 0f;
        Float f = 8000f;
        Float m = 0f;
        for(int i = 0; i < 30; i++){
            if(i < 20){
                m += f;
            }
            bj = m;
            float tmp = m * 0.04f;
            m += tmp;
            System.out.println("第"+(i+1)+"年，本金："+bj+", 当年利息："+tmp+"  总额："+m);
        }
    }

}
