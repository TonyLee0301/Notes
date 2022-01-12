package info.tonylee.studio.spring.circular;

import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

public class CircularTest {

    public static void main(String[] args) {

        XmlBeanFactory xmlBeanFactory = new XmlBeanFactory(new ClassPathResource("META-INF/circular/circular.xml"));
        System.out.println(xmlBeanFactory.getBean("testA"));
    }

}
