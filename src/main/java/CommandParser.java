import db.Database;
import config.RedisConfig;
import utils.RedisResponseBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CommandParser {
    private final Database database;
    CommandParser(Database database){
        this.database = database;
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
            default:
                response = null;
                break;
        }
        return response;
    }

    private String handleConfig(List<String> cmdArgs,RedisConfig redisConfig) {
        if(cmdArgs.get(1).equalsIgnoreCase("GET")){
            ArrayList<String> resCmdArr = new ArrayList<>();
            if(cmdArgs.get(2).equalsIgnoreCase("dir")){
                resCmdArr.add("dir");
                resCmdArr.add(redisConfig.getDirectory());
            }else{
                resCmdArr.add("dbfilename");
                resCmdArr.add(redisConfig.getDbFilename());
            }
            return RedisResponseBuilder.responseBuilder(resCmdArr);
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
