package info.tonyle.concurrent.chapter06;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class OutOfTimer {

    public static void main(String[] args) throws Exception{
        Timer timer = new Timer();
        timer.schedule(getTask(), 1000);
//        Thread.sleep(1000);
        timer.schedule(getTask(), 1000);
//        Thread.sleep(5000);
    }

    static TimerTask getTask(){
        return new RandomElapsedTimerTask();
    }

    static class ThrowTimerTask extends TimerTask{

        @Override
        public void run() {
            throw new RuntimeException();
        }
    }

    static class RandomElapsedTimerTask extends TimerTask{
        @Override
        public void run() {
            try {
                System.out.println("开始执行时间: " + System.currentTimeMillis());
                int seconds = new Random().nextInt(10);
                System.out.println(String.format("%s 将执行 %s 秒",Thread.currentThread().getName(),seconds));
                Thread.sleep(seconds * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
