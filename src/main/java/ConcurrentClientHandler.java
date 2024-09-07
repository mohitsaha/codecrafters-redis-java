import java.io.*;
import java.net.Socket;

public class ConcurrentClientHandler implements Runnable{
    private Socket clientSocket;
    public ConcurrentClientHandler(Socket clientSocket){
        this.clientSocket = clientSocket;
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
                System.out.printf("Recieved %s \n", input);
                if (input.equalsIgnoreCase("ping"))
                    out.write("+PONG\r\n");
                out.flush();
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
