package replication;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import common.SocketUtil;
import lombok.extern.slf4j.Slf4j;
import redis.RedisCommand;
import redis.RedisResultData;

@Slf4j
public class CommandSender {
    private CommandSender() {

    }
    public static String sendCommand(BufferedReader reader, BufferedWriter writer, RedisCommand redisCommand, List<String> args) {
        var inputParams = new ArrayList<String>();
        inputParams.add(redisCommand.name());
        inputParams.addAll(args);

        try {
            var sendMessage = RedisResultData.getArrayData(inputParams.toArray(new String[0]));
            SocketUtil.sendStringToSocket(writer, RedisResultData.convertToOutputString(sendMessage));

            var result = reader.readLine();

            log.info("send command complete - command: {}, args: {}, result: {}", redisCommand, args, result);
            return result;
        } catch (IOException e) {
            log.error("IOException", e);
            return null;
        }
    }
}