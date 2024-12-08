package redis;

import java.io.IOException;
import java.net.ServerSocket;

public class RedisConnectionUtil {
    private RedisConnectionUtil() {
    }

    /**
     * create Redis server socket.
     * It will close when program is terminated.
     * @param port
     * @return
     */
    public static ServerSocket createRedisServerSocket(int port) {
        try {
            var serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));

            return serverSocket;
        } catch (IOException e) {
            throw new RuntimeException(String.format("IOException: %s%n", e.getMessage()));
        }
    }
}