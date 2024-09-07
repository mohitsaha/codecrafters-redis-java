import java.io.BufferedReader;
import java.io.IOException;
import java.util.Locale;

public class CommandParser {
    private final Database database;
    CommandParser(Database database){
        this.database = database;
    }
    public String parseCommand(String input, BufferedReader reader) throws IOException {
        String response;
        switch (input.toUpperCase()){
            case "PING":
                response = handlePing();
                break;
            case "ECHO":
                reader.readLine();
                String message = reader.readLine();
                response = handleEcho(message);
                break;
            case "GET":
                reader.readLine();
                String getString = reader.readLine();
                response = handleGET(getString);
                break;
            case "SET":
                reader.readLine();
                String key = reader.readLine();
                reader.readLine();
                String value = reader.readLine();
                response = handleSet(key,value);
                break;
            default:
                response = null;
                break;
        }
        return response;
    }

    private String handleSet(String key, String value) {
        return database.set(key,value);
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
