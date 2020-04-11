package info.tonyle.concurrent.objectsharding.visibility;

public class NoVisibility {

    private static boolean ready;

    private static int number;

    private static class ReaderThread extends Thread{
        public void run(){
            while(!ready){
                Thread.yield();
            }
            System.out.println(number);
        }
    }

    public static void main(String[] args) {
        /**
         * 由于ready和number 没有使用 volatile 因此可能会导致以下问题：
         * 1. 重排序 出现 number 还未写入 42 但 ready 已经变为 true 使输出结果为0；
         * 2. ReaderThread 线程未读到 ready 写入的true，导致一直循环。
         * 因为ReaderThread线程会拷贝一份相关的变量到线程内部，
         */
        new ReaderThread().start();
        number = 42;
        ready = true;
    }
}
