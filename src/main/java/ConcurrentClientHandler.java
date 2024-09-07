import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ConcurrentClientHandler implements Runnable{
    private Socket clientSocket;
    private final ArrayList<byte[]> args;
    private final CommandParser commandParser;

    public ConcurrentClientHandler(Socket clientSocket){
        this.clientSocket = clientSocket;
        args = new ArrayList<>();
        commandParser = new CommandParser();
    }

    @Override
    public void run() {
        try {
            InputStream inputStream = clientSocket.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader br = new BufferedReader(inputStreamReader);
            OutputStreamWriter out_writer = new OutputStreamWriter(clientSocket.getOutputStream());
            BufferedWriter out = new BufferedWriter(out_writer);
            String input;
            while ((input = br.readLine()) != null) {
                String response = commandParser.parseCommand(input,br);
                if(response != null) {
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
