import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import common.RandomUtil;
import lombok.extern.slf4j.Slf4j;
import rdb.RdbBuilder;
import rdb.RdbUtil;
import redis.CommonConstant;
import redis.RedisConnectionThread;
import redis.RedisConnectionUtil;
import redis.RedisRepository;
import replication.ReplicationConstant;
import replication.ReplicationRole;
import replication.SlaveConnectionProvider;

@Slf4j
public class Main {
    public static void main(String[] args) throws IOException {
        var portNumber = init(args);
        var serverSocket = RedisConnectionUtil.createRedisServerSocket(portNumber);

        initReplica();
        log.info("Logs from your program will appear here!");

        while (true) {
            var clientSocket = serverSocket.accept();
            var redisClientThread = new RedisConnectionThread(clientSocket);
            redisClientThread.start();
        }
    }

    public static int init(String[] args) {
        parseConfig(args);

        var dir = RedisRepository.configGet("dir");
        var dbFileName = RedisRepository.configGet("dbfilename");
        var port = RedisRepository.configGet("port");

        if (dir != null && dbFileName != null) {
            try {
                var data = RdbUtil.openRdbFile(dir, dbFileName);
                var builder = new RdbBuilder().bytes(data);
                var rdb = builder.build();

                log.info("rdb: {}", rdb);
                if (rdb != null) {
                    rdb.init();
                }
            } catch (Exception e) {
                log.info("RDB Read Failed. init without RDB file.", e);
            }
        }

        if (port != null) {
            try {
                var portNumber = Integer.parseInt(port);

                return portNumber;
            } catch (Exception e) {
                log.info("Setting Custom Port Number Failed. use default port ({})", CommonConstant.DEFAULT_REDIS_PORT);
            }
        } else {
            RedisRepository.configSet("port", String.valueOf(CommonConstant.DEFAULT_REDIS_PORT));
        }

        return CommonConstant.DEFAULT_REDIS_PORT;
    }

    public static void initReplica() {
        var replicaOf = RedisRepository.getReplicationConfig(ReplicationConstant.REPLICATION_REPLICA_OF);

        if (replicaOf == null) {
            initReplicaForMaster();
        } else {
            initReplicaFoSlave();
        }
    }

    private static void initReplicaForMaster() {
        var replid = RandomUtil.createRandomString(40);

        RedisRepository.setReplicationSetting("role", ReplicationRole.MASTER.name().toLowerCase());
        RedisRepository.setReplicationSetting("master_replid", replid);
        RedisRepository.setReplicationSetting("master_repl_offset", "0");
    }

    private static void initReplicaFoSlave() {
        RedisRepository.setReplicationSetting("role", ReplicationRole.SLAVE.name().toLowerCase());

        var replicaOf = RedisRepository.getReplicationConfig(ReplicationConstant.REPLICATION_REPLICA_OF);
        var slaveConnectionProvider = new SlaveConnectionProvider();
        slaveConnectionProvider.init(replicaOf.getFirst(), Integer.parseInt(replicaOf.getLast()));
    }

    public static void parseConfig(String[] args) {
        var idx = 0;
        while (idx < args.length) {
            var keyword = args[idx++];

            if (ReplicationConstant.REPLICATION_CONFIG_LIST.contains(keyword)) {
                var values = new ArrayList<String>();
                while (idx < args.length && !args[idx].startsWith("--")) {
                    values.add(args[idx++]);
                }
                RedisRepository.setReplicationConfig(keyword.substring(2), List.of(values.getFirst().split(" ")));
            } else if (keyword.startsWith("--")) {
                var value = args[idx++];
                RedisRepository.configSet(keyword.substring(2), value);
            }
        }
    }
}