import config.RedisConfig;
import config.Role;
import db.RDBFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    public static int totalBytesProcessed=0;
    public static int currentProcessingBytes=0;

    public static void main(String[] args) {
        ServerSocket serverSocket;

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                20,
                30,
                500,
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
                logger.info("Recieving message on port : " + REDIS_DEFAULT_PORT);
            }else {
                serverSocket = new ServerSocket(config.getPortNumber());
                logger.info("Recieving message on port : " + config.getPortNumber());
            }
            serverSocket.setReuseAddress(true);
            Socket slaveSocket;
            //Slave ->
            //
            if(config != null && config.getRole() == Role.SLAVE){
                String host = config.getReplicaOfHost().split(" ")[0];
                int port = Integer.parseInt(config.getReplicaOfHost().split(" ")[1]);
                slaveSocket = new Socket(host,port);
                InputStream input = slaveSocket.getInputStream();
                OutputStream output = slaveSocket.getOutputStream();
                RedisCommandBuilder builder = new RedisCommandBuilder();
                System.out.println("sending intial commands as Slave");
                //as a slave the it sends this commands as inital handshake
                sendCommand(output, input, builder.command("PING").build()); // i am active
                sendCommand(output, input, builder.command("REPLCONF")
                        .argument("listening-port").argument(config.getPortNumber()).build()); // replica from this port
                sendCommand(output, input, builder.command("REPLCONF")
                        .argument("capa").argument("psync2").build());
                String psyncCmd = builder.command("PSYNC").argument("?").argument("-1").build();
                output.write(psyncCmd.getBytes());
                //SKIPPING "+FULLRESYNC 75cd7bc10c49047e0d163660f3b90625b1af31dc 0\r\n"
                //can write resp protocol parser instead of skipping these bytes
                //SKIPPING  Empty RDB FILE "$88\r\nREDIS0011\xfa\tredis-ver\x057.2.0\xfa\nredis-bits\xc0@\xfa\x05ctime\xc2m\b\xbce\xfa\bused-mem°\xc4\x10\x00\xfa\baof-base\xc0\x00\xff\xf0n;\xfe\xc0\xffZ\xa2"
                //RDB file processer is already in project instead of skipping hardcoded value
                input.skipNBytes(149);
                logger.info("starting slave Thread");
                threadPoolExecutor.execute(new ConcurrentClientHandler(slaveSocket, config));
            }
            while (true) {
                clientSocket = serverSocket.accept();
                logger.info("Starting client");
                ConcurrentClientHandler concurrentClientHandler = new ConcurrentClientHandler(clientSocket,config);
                threadPoolExecutor.execute(concurrentClientHandler);
            }
        } catch (IOException e) {
            logger.error("IOException: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                logger.error("IOException: " + e.getMessage());
            }
        }
    }
    public static String readResponse(InputStream input) throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead = input.read(buffer);
        if (bytesRead == -1) {
            throw new EOFException("End of stream reached");
        }
        String response = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
        System.out.println("Received response from slave inputstream : " + response);
        return response;
    }

    private static void sendCommand(OutputStream output, InputStream input, String command) throws Exception {
        output.write(command.getBytes(StandardCharsets.UTF_8));
        readResponse(input);
    }
    public static void printInputStream(InputStream inputStream) throws IOException {
        int byteRead;
        System.out.println("debug :: starting PrintStream ");

        // Read the input stream byte by byte
        while ((byteRead = inputStream.read()) != -1) {
            // Cast the byte to a char and print it
            System.out.print((char) byteRead);
        }
        System.out.println("debug :: ending PrintStream ");
    }


}
