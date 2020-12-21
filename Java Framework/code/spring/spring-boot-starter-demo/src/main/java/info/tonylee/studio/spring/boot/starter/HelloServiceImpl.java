package info.tonylee.studio.spring.boot.starter;

import org.springframework.stereotype.Component;

@Component
public class HelloServiceImpl implements HelloService {
    @Override
    public String sayHello() {
        return "say hello spring boot!";
    }
}
