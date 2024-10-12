import config.RedisConfig;
import config.Role;
import db.RDBFile;
import utils.RedisCommandBuilder;
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
                6,
                6,
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2)
        );

        RedisConfig config = parseArgs(args);
        if(config != null && config.getRole() == null){
            config.setRole(Role.MASTER);
        }
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
            Socket slaveSocket = null;
            if(config != null && config.getRole() == Role.SLAVE){
                String host = config.getReplicaOfHost().split(" ")[0];
                int port = Integer.parseInt(config.getReplicaOfHost().split(" ")[1]);
                slaveSocket = new Socket(host,port);
                InputStream input = slaveSocket.getInputStream();
                OutputStream output = slaveSocket.getOutputStream();
                RedisCommandBuilder builder = new RedisCommandBuilder();
                //as a slave the it sends this commands as inital handshake
                sendCommand(output, input, builder.command("PING"));
                sendCommand(output, input, builder.command("REPLCONF")
                        .argument("listening-port").argument(config.getPortNumber()));
                sendCommand(output, input, builder.command("REPLCONF")
                        .argument("capa").argument("psync2"));
                sendCommand(output, input, builder.command("PSYNC")
                        .argument("?").argument("-1"));
            }
            while (true) {
                clientSocket = serverSocket.accept();
                System.out.println("starting a client");
                ConcurrentClientHandler concurrentClientHandler = new ConcurrentClientHandler(clientSocket,config);
                threadPoolExecutor.execute(concurrentClientHandler);
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
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
    public static void readResponse(InputStream input) throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead = input.read(buffer);

        if (bytesRead == -1) {
            throw new EOFException("End of stream reached");
        }
        String response = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
        System.out.println("Received response from slave inputstream : " + response);
    }

    private static void sendCommand(OutputStream output, InputStream input, RedisCommandBuilder builder) throws Exception {
        String command = builder.build();
        output.write(command.getBytes(StandardCharsets.UTF_8));
        readResponse(input);
    }

}
