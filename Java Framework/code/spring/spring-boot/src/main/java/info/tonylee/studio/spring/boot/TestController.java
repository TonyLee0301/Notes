package info.tonylee.studio.spring.boot;

import info.tonylee.studio.spring.boot.starter.HelloService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @Autowired
    private HelloService helloService;
    @RequestMapping("/")
    String home(){
//        return "Hello Spring Boot";
        return helloService.sayHello();
    }
}
