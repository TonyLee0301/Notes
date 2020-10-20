package info.tonylee.studio.spring.beanDefinition;

import org.springframework.beans.factory.FactoryBean;

public class FactoryBeanTest implements FactoryBean<Person> {

    public Person getObject() throws Exception {
        return null;
    }

    public Class<?> getObjectType() {
        return Person.class;
    }
}
