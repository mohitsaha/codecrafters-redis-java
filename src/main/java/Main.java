import config.RedisConfig;
import db.RDBFile;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import static utils.Constants.REDIS_DEFAULT_PORT;
import static utils.RedisArgsParser.parseArgs;
import java.util.concurrent.*;
public class Main {
    public static void main(String[] args) {
        ServerSocket serverSocket;
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                2,
                4,
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2)
        );

        RedisConfig config = parseArgs(args);
        //checking in config if there is DB flag turned on
        if(config != null && config.getDbFilename() != null){
            RDBFile rdbFile = new RDBFile(config);
        }
        //Checking in config if it's master or slave
        Socket clientSocket = null;
        try {
            if(config == null || config.getPortNumber() == null){
                serverSocket = new ServerSocket(REDIS_DEFAULT_PORT);
                System.out.println("Recieving message on port : " + REDIS_DEFAULT_PORT);
            }else {
                serverSocket = new ServerSocket(config.getPortNumber());
                System.out.println("Recieving message on port : " + config.getPortNumber());
            }
            serverSocket.setReuseAddress(true);

            while (true) {
                clientSocket = serverSocket.accept();
                ConcurrentClientHandler concurrentClientHandler = new ConcurrentClientHandler(clientSocket,config);
                threadPoolExecutor.execute(concurrentClientHandler);
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


