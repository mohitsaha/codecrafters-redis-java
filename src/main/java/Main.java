import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;

public class Main {
  public static void main(String[] args){
        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        int port = 6379;
        try {
          serverSocket = new ServerSocket(port);
          // Since the tester restarts your program quite often, setting SO_REUSEADDR
          // ensures that we don't run into 'Address already in use' errors
          serverSocket.setReuseAddress(true);
          // Wait for connection from client.
          System.out.println("Recieving message on port : 6379");
          clientSocket = serverSocket.accept();
          InputStream inputStream = clientSocket.getInputStream();
          InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
          BufferedReader br = new BufferedReader(inputStreamReader);
          OutputStreamWriter out_writer = new OutputStreamWriter(clientSocket.getOutputStream());
          BufferedWriter out = new BufferedWriter(out_writer);
          String input;
          while((input = br.readLine()) != null) {
              System.out.printf("Recieved %s \n", input);
              if (input.equalsIgnoreCase("ping"))
                  out.write("+PONG\r\n");
              out.flush();
          }
        } catch (IOException e) {
          System.out.println("IOException: " + e.getMessage());
        } finally {
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
