package redis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import common.SocketUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import rdb.RdbUtil;
import replication.MasterConnectionHolder;

@Slf4j
@RequiredArgsConstructor
public class RedisExecutor {
    private final Socket socket;
    private final OutputStream outputStream;
    private final BufferedWriter writer;
    private final boolean isReplication;

    public boolean parseAndExecute(List<String> inputParams) {
        try {
            var command = RedisCommand.parseCommand(inputParams.getFirst());
            if (command == null) {
                returnCommonErrorMessage(null);
                return false;
            }

            var data = executeCommand(inputParams);
            if (data == null || isReplication) {
                return true;
            }
            var outputStr = RedisResultData.convertToOutputString(data);
            log.debug("output: {}", outputStr);

            if (command.isSendMessage()) {
                SocketUtil.sendStringToSocket(writer, outputStr);
            }
            if (command.isWrite()) {
                MasterConnectionHolder.propagateCommand(inputParams);
            }
            return true;
        } catch (RuntimeException e) {
            log.warn("command execute error - inputParams: {}", inputParams, e);
            returnCommonErrorMessage(null);
            return false;
        } catch (IOException e) {
            log.error("IOException", e);
            return false;
        }
    }

    public void returnCommonErrorMessage(String detailErrorMessage) {
        try {
            if (detailErrorMessage != null) {
                writer.write("-ERR " + detailErrorMessage + "\r\n");
            } else {
                writer.write("-ERR\r\n");
            }
            writer.flush();
        } catch (IOException e) {
            log.error("IOException", e);
        }
    }

    private List<RedisResultData> executeCommand(List<String> inputParams) {
        var command = RedisCommand.parseCommand(inputParams.getFirst());
        var restParams = inputParams.subList(1, inputParams.size());

        return switch (command) {
            case PING -> ping();
            case ECHO -> echo(restParams);
            case GET -> get(restParams);
            case SET -> set(restParams);
            case DEL -> del(restParams);
            case CONFIG -> config(restParams);
            case KEYS -> keys();
            case TYPE -> type(restParams);
            case INFO -> info(restParams);
            case REPLCONF -> replconf(restParams);
            case PSYNC -> psync(restParams);
            case WAIT -> wait(restParams);
        };
    }

    private List<RedisResultData> type(List<String> restParams) {
        var findResult = RedisRepository.get(restParams.get(0));
        if (findResult == null) {
            findResult = "none";
        }else{
            findResult = "string";
        }
        return List.of(new RedisResultData(RedisDataType.SIMPLE_STRINGS,findResult));
    }

    private List<RedisResultData> ping() {
        return List.of(new RedisResultData(RedisDataType.SIMPLE_STRINGS, CommonConstant.REDIS_OUTPUT_PONG));
    }

    private List<RedisResultData> echo(List<String> args) {
        if (args.size() != 1) {
            throw new RedisExecuteException("execute error - echo need exact 1 args");
        }

        return RedisResultData.getBulkStringData(args.getFirst());
    }

    private List<RedisResultData> get(List<String> args) {
        if (args.size() != 1) {
            throw new RedisExecuteException("execute error - get need exact 1 args");
        }

        var key = args.getFirst();
        var findResult = RedisRepository.get(key);

        return RedisResultData.getBulkStringData(findResult);
    }

    private List<RedisResultData> set(List<String> args) {
        if (args.size() < 2) {
            throw new RedisExecuteException("execute error - set need more than 2 args");
        }

        var key = args.getFirst();
        var value = args.get(1);
        var expireTime = new AtomicLong(-1L);

        // TODO: if need more options, extract separate method... maybe?
        if (args.size() >= 4) {
            if ("px".equalsIgnoreCase(args.get(2))) {
                var milliseconds = Long.parseLong(args.get(3));
                expireTime.set(milliseconds);
            }
        }

        RedisRepository.set(key, value);

        if (expireTime.get() > 0L) {
            RedisRepository.expireWithExpireTime(key, expireTime.get());
        }

        return RedisResultData.getSimpleResultData(RedisDataType.SIMPLE_STRINGS, CommonConstant.REDIS_OUTPUT_OK);
    }

    private List<RedisResultData> del(List<String> args) {
        var key = args.getFirst();

        RedisRepository.del(key);
        return RedisResultData.getSimpleResultData(RedisDataType.SIMPLE_STRINGS, CommonConstant.REDIS_OUTPUT_OK);
    }

    private List<RedisResultData> config(List<String> args) {
        if (args.size() != 2) {
            throw new RedisExecuteException("execute error - config need exact 2 params");
        }

        if (CommonConstant.REDIS_COMMAND_PARAM_GET.equalsIgnoreCase(args.getFirst())) {
            var key = args.get(1);
            var value = RedisRepository.configGet(key);
            return RedisResultData.getArrayData(key, value);
        } else {
            throw new RedisExecuteException("execute error - not supported option");
        }
    }

    private List<RedisResultData> keys() {
        var keys = RedisRepository.getKeys();
        return RedisResultData.getArrayData(keys.toArray(new String[0]));
    }

    private List<RedisResultData> info(List<String> restParam) {
        if (!restParam.getFirst().equalsIgnoreCase("replication")) {
            return null;
        }

        var result = new StringBuilder("# Replication\n");

        for (var setting : RedisRepository.getAllReplicationSettings()) {
            result.append(setting.getKey());
            result.append(":");
            result.append(setting.getValue());
            result.append("\n");
        }

        return RedisResultData.getBulkStringData(result.toString());
    }

    private List<RedisResultData> replconf(List<String> restParam) {
        log.info("REPLCONF INPUT - param: {}", restParam);
        if ("GETACK".equalsIgnoreCase(restParam.getFirst()) && "*".equalsIgnoreCase(restParam.get(1))) {
            try {
                var offset = RedisRepository.getReplicationSetting("a", "0");
                var message = RedisResultData.getArrayData("REPLCONF", "ACK", offset);

                SocketUtil.sendStringToSocket(writer, RedisResultData.convertToOutputString(message));
                return null;
            } catch (Exception e) {
                log.error("IOException", e);
            }
        } else if ("ACK".equalsIgnoreCase(restParam.getFirst()) && !isReplication) {
            var offset = Integer.parseInt(restParam.getLast());
            var connectionProvider = MasterConnectionHolder.findProvider(socket);
            connectionProvider.setPresentAck(offset);
            return null;
        }
        if ("listening-port".equalsIgnoreCase(restParam.getFirst()) || "capa".equalsIgnoreCase(restParam.getFirst())) {
            var host = socket.getInetAddress().getHostAddress();
            var connectionPort = socket.getPort();
            if(!"capa".equalsIgnoreCase(restParam.getFirst())) {
                var port = Integer.parseInt(restParam.get(1));
                log.info("ipAddress: {}, innerPort: {}, port:{}", host, connectionPort, port);
                // TODO: Will be used... maybe?
            }
            return RedisResultData.getSimpleResultData(RedisDataType.SIMPLE_STRINGS, "OK");
        }
        return null;
    }

    private List<RedisResultData> psync(List<String> restParam) {
        var replId = RedisRepository.getReplicationSetting("master_replid");
        var offset = RedisRepository.getReplicationSetting("master_repl_offset");

        var result = RedisResultData.getSimpleResultData(RedisDataType.SIMPLE_STRINGS, "FULLRESYNC %s %s".formatted(replId, offset));
        var message = Base64.getDecoder().decode(RdbUtil.EMPTY_RDB);
        var sizeData = new RedisResultData(RedisDataType.BULK_STRINGS, String.valueOf(message.length));

        result = Stream.concat(result.stream(), Stream.of(sizeData)).toList();

        try {
            SocketUtil.sendStringToSocket(writer, RedisResultData.convertToOutputString(result));
            SocketUtil.sendBytesToSocket(outputStream, message);

            var ipAddress = socket.getInetAddress().getHostAddress();
            var innerPort = socket.getPort();

            log.info("ipAddress: {}, innerPort: {}", ipAddress, innerPort);
            MasterConnectionHolder.addConnectedList(socket, writer);
        } catch (Exception e) {
            log.error("IOException", e);
        }

        return result;
    }

    private List<RedisResultData> wait(List<String> args) {
        if (args.size() != 2) {
            throw new RedisExecuteException("execute error - wait need exact 2 params");
        }

        var needReplica = Integer.parseInt(args.getFirst());
        var timeLimit = Integer.parseInt(args.getLast());

        return RedisResultData.getSimpleResultData(RedisDataType.INTEGER, String.valueOf(MasterConnectionHolder.getFullySyncedReplicaCount(needReplica, timeLimit)));
    }
}