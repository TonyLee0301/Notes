package info.tonylee.spring.test;

import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;

public class TestAnnotationUtils {

    public static void main(String[] args) {
        Annotation annotation = AnnotationUtils.findAnnotation(B.class, Aspect.class);
        System.out.println(annotation instanceof Aspect);
        annotation = AnnotationUtils.findAnnotation(B.class, Component.class);
        System.out.println(annotation instanceof Component);
        annotation = AnnotationUtils.findAnnotation(B.class, Deprecated.class);
        System.out.println(annotation);
    }

}

@Component
interface I{

}
@Aspect
class A{

}

class B extends A implements I{

}