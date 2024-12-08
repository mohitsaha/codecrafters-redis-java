package redis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.List;

import common.Pair;
import common.SocketUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class RedisConnectionThread extends Thread {
    private final Socket socket;

    @Override
    public void run() {
        try (
                var inputStream = socket.getInputStream();
                var outputStream = socket.getOutputStream();
                var bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                var bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
        ) {
            Pair<Integer, List<String>> inputInfo;
            List<String> inputParams;
            var redisExecutor = new RedisExecutor(socket, outputStream, bufferedWriter, false);
            while ((inputInfo = SocketUtil.parseSocketInputToRedisCommand(bufferedReader)) != null) {
                inputParams = inputInfo.second();
                if (inputParams.isEmpty()) {
                    continue;
                }
                log.debug("inputParams: {}", inputParams);
                redisExecutor.parseAndExecute(inputParams);
            }
        } catch (IOException e) {
            log.error("create I/O stream error.", e);
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                log.error("close socket error.", e);
            }
        }
    }
}