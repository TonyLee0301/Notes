package info.tonyle.concurrent.chapter06;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class LifecycleWebServer {
    private static final int NTHREADS = 100;
    private static final ExecutorService exec = Executors.newFixedThreadPool(NTHREADS);

    public void start() throws IOException {
        ServerSocket socket = new ServerSocket(80);
        while(true){
            try{
                final Socket connection = socket.accept();
                exec.execute(()->{
                    handleRequest(connection);
                });
            }catch (RejectedExecutionException e){
                if(!exec.isShutdown()){
                    System.out.println("task submission rejected");
                    e.printStackTrace();
                }
            }

        }
    }

    public void stop(){exec.shutdown();}



    private void handleRequest(Socket connection){
        //按照特定协议如果出现请求关闭则调用stop
        //if(){stop();}
        return;
    }
}
