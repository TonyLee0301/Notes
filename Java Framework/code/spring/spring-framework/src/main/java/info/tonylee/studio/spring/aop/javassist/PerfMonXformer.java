package info.tonylee.studio.spring.aop.javassist;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class PerfMonXformer implements ClassFileTransformer {
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        byte[] transformed = null;
        System.out.println("Transforming " + className);
        ClassPool pool = ClassPool.getDefault();
        CtClass cl = null;
        try {
            cl = pool.makeClass(new java.io.ByteArrayInputStream(classfileBuffer));
            if(cl.isInterface() == false) {
                CtBehavior[] methods = cl.getDeclaredBehaviors();
                for(int i = 0; i < methods.length; i++) {
                    if(methods[i].isEmpty() == false) {

                    }
                }
                transformed = cl.toBytecode();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(cl != null){
                cl.detach();
            }
        }
        return transformed;
    }

    private void doMethod(CtBehavior method) throws CannotCompileException {
        method.insertBefore("long stime = System.nanoTime();");
        method.insertAfter("System.out.println(\"leave " + method.getName() + " and time: \"+ (System.nanoTime()-stime)));");
    }
}
