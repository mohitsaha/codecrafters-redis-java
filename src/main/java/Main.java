import config.RedisConfig;
import config.Role;
import db.RDBFile;
import utils.RedisResponseBuilder;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

            if(config != null && config.getRole() == Role.SLAVE){
                //send ping to master
                String host = config.getReplicaOffHost().split(" ")[0];
                int port = Integer.parseInt(config.getReplicaOffHost().split(" ")[1]);
                Socket slaveSocket = new Socket(host,port);
                OutputStream output = slaveSocket.getOutputStream();
                String handShakeMsg = "*1\r\n$4\r\nping\r\n";
                output.write(handShakeMsg.getBytes(StandardCharsets.UTF_8));

                //# REPLCONF listening-port <PORT>
                //*3\r\n$8\r\nREPLCONF\r\n$14\r\nlistening-port\r\n$4\r\n6380\r\n

                //# REPLCONF capa psync2
                //*3\r\n$8\r\nREPLCONF\r\n$4\r\ncapa\r\n$6\r\npsync2\r\n
                ArrayList<String> cmdArray = new ArrayList<>();
                cmdArray.add("REPLCONF");
                cmdArray.add("listening-port");
                cmdArray.add(Integer.toString(config.getPortNumber()));
                output.write(RedisResponseBuilder.responseBuilder(cmdArray).getBytes(StandardCharsets.UTF_8));
                cmdArray.clear();

                cmdArray.add("REPLCONF");
                cmdArray.add("capa");
                cmdArray.add("psync2");
                output.write(RedisResponseBuilder.responseBuilder(cmdArray).getBytes(StandardCharsets.UTF_8));
                //PSYNC ? -1

                cmdArray.add("PSYNC");
                cmdArray.add("?");
                cmdArray.add("-1");
                output.write(RedisResponseBuilder.responseBuilder(cmdArray).getBytes(StandardCharsets.UTF_8));

            }
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
