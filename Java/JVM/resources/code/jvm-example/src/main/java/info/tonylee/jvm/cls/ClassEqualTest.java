package info.tonylee.jvm.cls;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * 比较两个类是否“相等”，只有在这两个类是同一个类加载器加载的前提下才有意义，
 * 否则，即时这两个类来源于同一个Class文件，被同一个Java虚拟机加载，只要加载它们的类加载器不同，那么这两个类就必定不相等
 *
 * @Author：Tony Lee
 * @Date：2021/12/20 2:31 下午
 */
public class ClassEqualTest {

    public static void main(String[] args) throws Exception {
        ClassLoader myLoader = new ClassLoader() {
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

        Object obj = myLoader.loadClass(ClassEqualTest.class.getName()).newInstance();

        System.out.println(obj.getClass());
        System.out.println(obj instanceof ClassEqualTest);

    }

}
