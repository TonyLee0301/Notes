package info.tonylee.studio.spring.aop.javassist;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;

public class PerfMonAgent {
    private static Instrumentation inst = null;

    /**
     * 当此代理程序指定给Java VM时，将在调用应用程序的主方法之前调用此方法。
     * @param agentArgs
     * @param _inst
     */
    public static void premain(String agentArgs, Instrumentation _inst){
        System.out.println("PerfMonAgent.premain() was called.");
        inst = _inst;
        ClassFileTransformer trans = new PerfMonXformer();
        System.out.println("Adding a PerfMonXformer instance to the JVM");
        inst.addTransformer(trans);
    }
}
