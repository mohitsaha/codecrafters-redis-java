package redis;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum RedisCommand {
    // Basic Command
    ECHO(true, false),
    SET(true, true),
    GET(true, false),
    DEL(true, true),
    CONFIG(true, false),
    KEYS(true, false),
    TYPE(true,false),

    // Replication Command
    INFO(true, false),
    PING(true, false),
    REPLCONF(true, false),
    PSYNC(false, false),
    WAIT(true, false),

    //Stream command
    XADD(true,true);

    private static final Map<String, RedisCommand> commandMap = Arrays.stream(RedisCommand.values())
            .collect(Collectors.toMap(redisCommand -> redisCommand.name().toLowerCase(), it -> it));

    public static RedisCommand parseCommand(String command) {
        return commandMap.get(command.toLowerCase());
    }

    private final boolean isSendMessage;
    private final boolean isWrite;
}