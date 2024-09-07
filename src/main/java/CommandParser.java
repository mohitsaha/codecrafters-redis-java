import java.io.BufferedReader;
import java.io.IOException;

public class CommandParser {
    public String parseCommand(String input, BufferedReader reader) throws IOException {
        String response;
        if (input.equalsIgnoreCase("PING")) {
            response = handlePing();
        } else if (input.equalsIgnoreCase("ECHO")) {
            reader.readLine();
            String message = reader.readLine();
            response = handleEcho(message);
        } else {
            System.out.println("-ERR unknown command\r\n");
            response = null;
        }
        return response;
    }

    private String handlePing() {
        return "+PONG\r\n";
    }

    private String handleEcho(String message) {
        return String.format("$%d\r\n%s\r\n", message.length(), message);
    }
}
