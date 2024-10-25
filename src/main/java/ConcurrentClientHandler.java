import config.RedisConfig;
import config.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.StreamHolder;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
public class ConcurrentClientHandler implements Runnable{
    private static final Logger logger = LoggerFactory.getLogger(ConcurrentClientHandler.class);
    private Socket clientSocket;
    private RedisConfig redisConfig;
    CommandParser commandParser = new CommandParser();
    public ConcurrentClientHandler(Socket clientSocket,RedisConfig redisConfig){
        this.clientSocket = clientSocket;
        this.redisConfig = redisConfig;
    }
    @Override
    public void run() {
        try {
            InputStream inputStream = clientSocket.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader br = new BufferedReader(inputStreamReader);
            OutputStreamWriter out_writer = new OutputStreamWriter(clientSocket.getOutputStream());
            BufferedWriter out = new BufferedWriter(out_writer);
            StreamHolder.outputStream.set(new PrintStream(clientSocket.getOutputStream()));
            StreamHolder.inputStream.set(clientSocket.getInputStream());
            StreamHolder.socket.set(clientSocket);
            //but in this input response
            logger.info("one client, handling command one by one");
            while(true) {
                String numOfElementsLine = br.readLine();
                if (numOfElementsLine == null) {
                    logger.info("Connection closed or no data received from the client.");
                    return;
                }
                Main.currentProcessingBytes += numOfElementsLine.length()+2;
                int numOfElements = Integer.parseInt(numOfElementsLine.substring(1));
                logger.info("numOfElements = " + numOfElements);
                List<String> commandArguments = new ArrayList<>();
                for (int i = 0; i < numOfElements; i++) {
                    String argumentSizeLine = br.readLine();
                    Main.currentProcessingBytes += argumentSizeLine.length()+2;
                    int argumentSize = Integer.parseInt(argumentSizeLine.substring(1));
                    String argument = br.readLine();
                    Main.currentProcessingBytes += argument.length()+2;
                    commandArguments.add(argument);
                    logger.info("Collected argument: " + argument);
                }
                logger.info("commands are : " + commandArguments);
                String response = commandParser.parseCommand(commandArguments,redisConfig);
                if (response != null) {
                    out.write(response);
                    out.flush();
                }
                Main.totalBytesProcessed += Main.currentProcessingBytes;
                Main.currentProcessingBytes=0;
            }
        }catch (IOException e ){
            logger.error("Exception while handling client :: {}", e.getMessage());
        }finally{
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                logger.error("IOException: " + e.getMessage());
            }
        }
    }
}
