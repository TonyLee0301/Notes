package info.tonylee.studio.spring.aop.javassist;

public class AppAgent {

    public void test(){
        System.out.println("Hello World!");
    }

    public static void main(String[] args){
        new AppAgent().test();
    }

}
