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

public class CommandParser {
    private final Database database = InMemoryDB.getInstance();;
    CommandParser(){

    }
    public String parseCommand(List<String> commandArguments, RedisConfig redisConfig) throws IOException {
        String response;
        switch (commandArguments.get(0).toUpperCase()){
            case "PING":
                response = handlePing();
                break;
            case "ECHO":
                response = handleEcho(commandArguments.get(1));
                break;
            case "GET":
                response = handleGET(commandArguments.get(1));
                break;
            case "SET":
                response = handleSet(commandArguments);
                break;
            case "CONFIG":
                response = handleConfig(commandArguments,redisConfig);
                break;
            case "KEYS":
                response = handleKeys(commandArguments,redisConfig);
                break;
            default:
                response = null;
                break;
        }
        return response;
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

}
