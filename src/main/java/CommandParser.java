import config.Role;
import db.Database;
import config.RedisConfig;
import db.InMemoryDB;
import utils.StreamHolder;
import utils.RedisCommandBuilder;
import utils.RedisResponseBuilder;

import java.io.*;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import static utils.RedisResponseBuilder.responseBuilder;
import static utils.RedisResponseBuilder.wrapper;

public class CommandParser {
    private final Database database = InMemoryDB.getInstance();
    private static BlockingQueue<String> blockingQueue = new LinkedBlockingDeque<>();
    public String parseCommand(List<String> commandArguments, RedisConfig redisConfig) throws IOException {

        String response;
        String command = commandArguments.get(0);
        if (command.equals("-p")) {
            if (commandArguments.size() > 1) {
                response = handleCustomPort(commandArguments, redisConfig);
            } else {
                response = "ERROR: Missing port number for -p";
            }
        }else {
            response = switch (command.toUpperCase()) {
                case "PING" -> handlePing();
                case "ECHO" -> handleEcho(commandArguments.get(1));
                case "GET" -> handleGET(commandArguments.get(1));
                case "SET" -> handleSet(commandArguments,redisConfig);
                case "CONFIG" -> handleConfig(commandArguments, redisConfig);
                case "KEYS" -> handleKeys(commandArguments, redisConfig);
                case "INFO" -> handleInfo(commandArguments, redisConfig);
                case "REPLCONF" -> "+OK\r\n";
                case "PSYNC" -> handlePsync(commandArguments,redisConfig);
                default -> "ERROR: Unknown command";
            };
        }

        return response;
    }

    private String handlePsync(List<String> commandArguments, RedisConfig redisConfig) {
        PrintStream out = StreamHolder.outputStream.get();
        redisConfig.addReplicaOutputStream(StreamHolder.outputStream.get());
        System.out.println("Redis Replicas are : "+redisConfig.getReplicas());
        String fullResyncResponse = "+FULLRESYNC 8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb 0\r\n";
        out.print(fullResyncResponse);
        out.flush();
        sendEmptyRDBFile();
        //sendBufferToCommand
        //sendCommandToReplica();

        return null;    
    }

    private void sendCommandToReplica() {
        PrintStream out = StreamHolder.outputStream.get();
        try {
            while (true) {
                String element = blockingQueue.take();
                out.write(element.getBytes());
                out.flush();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendEmptyRDBFile() {
        PrintStream out = StreamHolder.outputStream.get();
        String fileContents =
                "UkVESVMwMDEx+glyZWRpcy12ZXIFNy4yLjD6CnJlZGlzLWJpdHPAQPoFY3RpbWXCbQi8ZfoIdXNlZC1tZW3CsMQQAPoIYW9mLWJhc2XAAP/wbjv+wP9aog==";
        byte[] bytes = Base64.getDecoder().decode(fileContents);
        try {
            out.write(("$" + bytes.length + "\r\n").getBytes());
            out.write(bytes);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private String handleCustomPort(List<String> commandArguments, RedisConfig redisConfig) {
        String port = commandArguments.get(1);
        System.out.println("handling custom port, Port is " + port);
        String cmd = commandArguments.get(2);
        System.out.println("for command "+ cmd);
        commandArguments.remove(0);  // Remove -p
        commandArguments.remove(0);  // Remove port number
        if(cmd.equalsIgnoreCase("info")){
            return handleInfo(commandArguments,redisConfig);
        }
        throw new IllegalStateException("handle CustomPort Not implemented for commands");
    }

    private String handleKeys(List<String> cmdArgs,RedisConfig config) {
        if(cmdArgs.get(1).equalsIgnoreCase("*")) {
            return responseBuilder(database.getKeys());
        }
        return null;
    }

    private String handleConfig(List<String> cmdArgs,RedisConfig redisConfig) {
        RedisCommandBuilder builder = new RedisCommandBuilder();
        if(cmdArgs.get(1).equalsIgnoreCase("GET")){
            if(cmdArgs.size() == 3) {
                if (cmdArgs.get(2).equalsIgnoreCase("dir")) {
                    builder.command("dir");
                    builder.argument(redisConfig.getDirectory());
                } else {
                    builder.command("dbfilename");
                    builder.argument(redisConfig.getDbFilename());
                }
            }else if(cmdArgs.size() == 4){
                builder.command("dir");
                builder.argument(redisConfig.getDirectory());
                builder.command("dbfilename");
                builder.argument(redisConfig.getDbFilename());
            }
            return builder.build();
        }else{
            System.out.println("Method not implemented");
            return null;
        }
    }

    private String handleSet(List<String> cmdArgs,RedisConfig redisConfig) {
        if(redisConfig != null && redisConfig.getRole() == Role.MASTER){
                //Sending Data to replicas
            Set<OutputStream> replicaOS = redisConfig.getReplicas();
            System.out.println("Sending command from Master");
            replicaOS.forEach(outputStream -> {
                String response = RedisResponseBuilder.responseBuilder(cmdArgs);
                try {
                    outputStream.write(response.getBytes());
                    outputStream.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        String response;
        if(cmdArgs.size() == 3) {
            response =  database.set(cmdArgs.get(1), cmdArgs.get(2));
        }else{
            response =  database.set(cmdArgs);
        }
        return response;
    }

    private String handleGET(String data) {
        return database.get(data);
    }

    private String handlePing() {
        return "+PONG\r\n";
    }

    private String handleEcho(String message) {
        return String.format("$%d\r\n%s\r\n", message.length(), message);
    }

    private String handleInfo(List<String> commandArguments, RedisConfig redisConfig){
        String state = commandArguments.get(1);
        if(state.equals("replication")){
            if(redisConfig == null || redisConfig.getRole() == Role.MASTER) {
                return wrapper("role:master\nmaster_replid:8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb\nmaster_repl_offset:0");
            }else{
                return wrapper("role:slave");
            }
        }
        throw new IllegalArgumentException("Not implemented INFO for other states");
    }

}
