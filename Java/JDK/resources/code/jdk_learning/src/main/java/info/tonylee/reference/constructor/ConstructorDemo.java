package info.tonylee.reference.constructor;

import info.tonylee.reference.ReferenceObject;

import java.lang.reflect.Constructor;

public class ConstructorDemo {

    public static void getConstructor() throws NoSuchMethodException {
        Constructor<?>[] constructors = ReferenceObject.class.getDeclaredConstructors();
        System.out.println(constructors[0]);
        System.out.println(ReferenceObject.class.getDeclaredConstructor(Integer.class, ReferenceObject.SubObject.class).getDeclaringClass());
    }

    public static void main(String[] args) throws NoSuchMethodException {
        getConstructor();
    }
}
