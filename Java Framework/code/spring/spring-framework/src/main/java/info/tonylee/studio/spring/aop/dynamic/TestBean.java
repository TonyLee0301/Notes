package info.tonylee.studio.spring.aop.dynamic;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class TestBean {
    private String testStr = "testStr";

    public String getTestStr() {
        return testStr;
    }

    public void setTestStr(String testStr) {
        this.testStr = testStr;
    }

    public void test(){
        System.out.println("test");
    }

    public void test1(){
        System.out.println("test1");
        test();
    }

    public static void main(String[] args) {
        ApplicationContext bf = new ClassPathXmlApplicationContext("/META-INF/aop_test/test-aop.xml");
        TestBean testBean = bf.getBean(TestBean.class);
        System.out.println(testBean);
        testBean.test();
        System.out.println("--------------");
        testBean.test1();

    }

}
