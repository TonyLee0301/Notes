package info.tonylee.studio.spring.aop.dynamic.jdkDynamic;

public class UserServiceImpl implements UserService {
    public void add() {
        System.out.println("——————add——————");
    }

    @Override
    public void test() {
        System.out.println("-------test-------");
        add();
    }

    public static void main(String[] args) {
        UserService userService = new UserServiceImpl();
        MyInvocationHandler invocationHandler = new MyInvocationHandler(userService);
        UserService proxy = (UserService) invocationHandler.getProxy();
        proxy.test();
        System.out.println("----------------");
        proxy.add();
    }

}
