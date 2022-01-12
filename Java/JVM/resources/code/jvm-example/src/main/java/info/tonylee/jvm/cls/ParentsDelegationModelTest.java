package info.tonylee.jvm.cls;

import java.io.IOException;
import java.io.InputStream;
import java.util.Observer;

/**
 * @Author：Tony Lee
 * @Date：2021/12/21 10:55 上午
 */
public class ParentsDelegationModelTest {

    static ClassLoader myLoader = new ClassLoader() {
        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            try{
                String fileName = name.substring(name.lastIndexOf(".")+1)+".class";
                InputStream is = getClass().getResourceAsStream(fileName);
                if(is == null){
                    return super.loadClass(name);
                }
                byte[] b = new byte[is.available()];
                is.read(b);
                return defineClass(name, b, 0 , b.length);
            }catch (IOException e){
                throw new ClassNotFoundException(name);
            }
        }
    };

    public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException {

        Class clas_ob = myLoader.loadClass(ObServerTest.class.getName());
        System.out.println(clas_ob.getClassLoader());
        System.out.println(clas_ob.getSuperclass().getClassLoader());
        System.out.println(Observer.class.isAssignableFrom(clas_ob));

        System.out.println("----------");

        Class clas = myLoader.loadClass(CallableImpl.class.getName());
        System.out.println(clas.getInterfaces()[0].getClassLoader());
        System.out.println(Callable.class.isAssignableFrom(clas));
        System.out.println(Callable.class.isAssignableFrom(CallableImpl.class));

        System.out.println("----------");

        System.out.println(MyLoaderUser.class.getClassLoader());
        System.out.println(Object.class.getClassLoader());
        System.out.println(Object.class.isAssignableFrom(MyLoaderUser.class));
        Callable cal = (Callable)clas.newInstance();
        new MyLoaderUser(cal).test("1234");
    }
}

class MyLoaderUser{
    private Callable callable;
    public MyLoaderUser(Callable callback){
        callable = callback;
    }
    public void test(Object obj){
        System.out.println(MyLoaderUser.class.getName());
        callable.call(obj);
    }
}
