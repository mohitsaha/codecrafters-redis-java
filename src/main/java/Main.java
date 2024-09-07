import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2,            // core pool size
                4,            // maximum pool size
                60,           // keep-alive time for idle threads
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2) // work queue
        );
        Socket clientSocket = null;
        int port = 6379;
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            System.out.println("Recieving message on port : 6379");
            while (true) {
                clientSocket = serverSocket.accept();
                ConcurrentClientHandler concurrentClientHandler = new ConcurrentClientHandler(clientSocket);
                executor.execute(concurrentClientHandler);
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } finally {
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
            }
        }
    }
}
