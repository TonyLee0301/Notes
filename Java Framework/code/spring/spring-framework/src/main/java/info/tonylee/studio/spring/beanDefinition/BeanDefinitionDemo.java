package info.tonylee.studio.spring.beanDefinition;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

public class BeanDefinitionDemo {

    public static void main(String[] args) {

        ConfigurableListableBeanFactory bf = new XmlBeanFactory(new ClassPathResource("/META-INF/beanDefinition.xml"));

        for(String beanName : bf.getBeanDefinitionNames()){
            System.out.println( beanName +" " + bf.getBeanDefinition(beanName));
        }

    }

}
