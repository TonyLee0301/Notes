package info.tonylee.studio.spring;

import java.util.Random;

public class Test {

    public static void main(String[] args) {
        Random random = new Random();
        float[] lvs = new float[]{};
        Float bj = 0f;
        Float t = 12000f;
        Float bjlx = 0f;
        for(int i = 0; i < 30; i++){
            if(i < 20){
                bj += t;
                bjlx += t;
            }
            float lv = 0.035f;
            bjlx += bjlx * lv;
            System.out.println(String.format("第%s年，利率：%s,本金：%s，保费：%s, 本息合计：%s",(i+1),lv, bj,bj*1.6f,bjlx));
        }
    }

}
