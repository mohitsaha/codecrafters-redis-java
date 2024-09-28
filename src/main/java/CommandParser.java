import db.Database;
import config.RedisConfig;
import db.InMemoryDB;
import utils.RedisResponseBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static utils.RedisResponseBuilder.responseBuilder;
import static utils.RedisResponseBuilder.wrapper;

public class CommandParser {
    private final Database database = InMemoryDB.getInstance();

    public String parseCommand(List<String> commandArguments, RedisConfig redisConfig) throws IOException {
        String response;
        String command = commandArguments.get(0);

        // Handle cases like "-p" directly without converting to uppercase
        if (command.equals("-p")) {
            if (commandArguments.size() > 1) {
                response = handleCustomPort(commandArguments, redisConfig);
            } else {
                response = "ERROR: Missing port number for -p";
            }
        } else {
            response = switch (command.toUpperCase()) {
                case "PING" -> handlePing();
                case "ECHO" -> handleEcho(commandArguments.get(1));
                case "GET" -> handleGET(commandArguments.get(1));
                case "SET" -> handleSet(commandArguments);
                case "CONFIG" -> handleConfig(commandArguments, redisConfig);
                case "KEYS" -> handleKeys(commandArguments, redisConfig);
                case "INFO" -> handleInfo(commandArguments, redisConfig);
                default -> "ERROR: Unknown command";
            };
        }

        return response;
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
        if(cmdArgs.get(1).equalsIgnoreCase("GET")){
            ArrayList<String> resCmdArr = new ArrayList<>();
            if(cmdArgs.size() == 3) {
                if (cmdArgs.get(2).equalsIgnoreCase("dir")) {
                    resCmdArr.add("dir");
                    resCmdArr.add(redisConfig.getDirectory());
                } else {
                    resCmdArr.add("dbfilename");
                    resCmdArr.add(redisConfig.getDbFilename());
                }
            }else if(cmdArgs.size() == 4){
                resCmdArr.add("dir");
                resCmdArr.add(redisConfig.getDirectory());
                resCmdArr.add("dbfilename");
                resCmdArr.add(redisConfig.getDbFilename());
            }
            return responseBuilder(resCmdArr);
        }else{
            System.out.println("Method not implemented");
            return null;
        }
    }

    private String handleSet(List<String> cmdArgs) {
        if(cmdArgs.size() == 3) {
            return database.set(cmdArgs.get(1), cmdArgs.get(2));
        }else{
            return database.set(cmdArgs);
        }
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
            //role:master
            //connected_slaves:0
            //master_replid:8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb
            //master_repl_offset:0
            //second_repl_offset:-1
            //repl_backlog_active:0
            //repl_backlog_size:1048576
            //repl_backlog_first_byte_offset:0
            //repl_backlog_histlen:
//            ArrayList<String> resArr = new ArrayList<>();
//            resArr.add("# Replication");
//            resArr.add("role:master");
//            return responseBuilder(resArr);
            return wrapper("role:master");
        }
        throw new IllegalArgumentException("Not implemented INFO for other states");
    }

}
