import db.Database;
import config.RedisConfig;
import utils.RedisResponseBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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
            File file = new File(config.getDirectory(), config.getDbFilename());
            try (InputStream fis = new FileInputStream(file)) {
                byte[] redis = new byte[5];
                byte[] version = new byte[4];
                fis.read(redis);
                fis.read(version);
                System.out.println("Magic String = " +
                        new String(redis, StandardCharsets.UTF_8));
                System.out.println("Version = " +
                        new String(version, StandardCharsets.UTF_8));
                int b;
                header:
                while ((b = fis.read()) != -1) {
                    switch (b) {
                        case 0xFF:
                            System.out.println("EOF");
                            break;
                        case 0xFE:
                            System.out.println("SELECTDB");
                            break;
                        case 0xFD:
                            System.out.println("EXPIRETIME");
                            break;
                        case 0xFC:
                            System.out.println("EXPIRETIMEMS");
                            break;
                        case 0xFB:
                            System.out.println("RESIZEDB");
                            b = fis.read();
                            fis.readNBytes(lengthEncoding(fis, b));
                            fis.readNBytes(lengthEncoding(fis, b));
                            break header;
                        case 0xFA:
                            System.out.println("AUX");
                            break;
                    }
                }

                System.out.println("header done");

                while ((b = fis.read()) != -1) {
                    System.out.println("value-type = " + b);
                    b = fis.read();
                    System.out.println("value-type = " + b);
                    System.out.println("b = " + Integer.toBinaryString(b));
                    System.out.println("reading keys");
                    int strLength = lengthEncoding(fis, b);
                    b = fis.read();
                    System.out.println("strLength == " + strLength);
                    if (strLength == 0) {
                        strLength = b;
                    }
                    System.out.println("strLength == " + strLength);
                    byte[] bytes = fis.readNBytes(strLength);
                    ArrayList<String> ans = new ArrayList<>();
                    ans.add(new String(bytes));
                    return RedisResponseBuilder.responseBuilder(ans);
                }
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
            }
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
    private static int lengthEncoding(InputStream is, int b) throws IOException {
        int length = 0;
        int first2bits = b & 0xC0;
        if (first2bits == 0x00) {
            System.out.println("00");
            length = 0;
        } else if (first2bits == 0x40) {
            System.out.println("01");
            length = 2;
        } else if (first2bits == 0x80) {
            System.out.println("10");
            ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.put(is.readNBytes(4));
            buffer.rewind();
            length = buffer.getInt();
        } else if (first2bits == 0xC0) {
            System.out.println("11");
            length = -1;
        }
        return length;
    }

}
