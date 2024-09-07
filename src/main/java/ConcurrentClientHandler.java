import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ConcurrentClientHandler implements Runnable{
    private Socket clientSocket;

    private final CommandParser commandParser;
    private final Database db;
    public ConcurrentClientHandler(Socket clientSocket){
        this.clientSocket = clientSocket;
        db = new InMemoryDB();
        commandParser = new CommandParser(db);
    }
    @Override
    public void run() {
        try {
            InputStream inputStream = clientSocket.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader br = new BufferedReader(inputStreamReader);
            OutputStreamWriter out_writer = new OutputStreamWriter(clientSocket.getOutputStream());
            BufferedWriter out = new BufferedWriter(out_writer);
            while(true) {
                String numOfElementsLine = br.readLine();
                if (numOfElementsLine == null) {
                    System.err.println("Connection closed or no data received from the client.");
                    return;
                }
                int numOfElements = Integer.parseInt(numOfElementsLine.substring(1)); // *5\r\n
                System.out.println("numOfElements = " + numOfElements);
                // Collect the command arguments
                List<String> commandArguments = new ArrayList<>();
                for (int i = 0; i < numOfElements; i++) {
                    String argumentSizeLine = br.readLine(); // Read the size line (e.g., $3\r\n)
                    int argumentSize = Integer.parseInt(argumentSizeLine.substring(1)); // Get the size, e.g., 3 for $3\r\n
                    String argument = br.readLine(); // Read the actual argument (e.g., SET)
                    commandArguments.add(argument);  // Add the command or argument to the list
                    System.out.println("Collected argument: " + argument);
                }
                System.out.println("commands are : " + commandArguments);
                String response = commandParser.parseCommand(commandArguments);
                if (response != null) {
                    out.write(response);
                    out.flush();
                }
            }
        }catch (IOException e ){
            System.err.printf("Exception while handling client :: {}", e.getMessage());
        }finally{
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
            }
        }
    }
}
