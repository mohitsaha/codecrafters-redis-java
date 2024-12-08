package replication;

import java.util.List;

public class ReplicationConstant {
    private ReplicationConstant() {

    }

    public static List<String> REPLICATION_CONFIG_LIST = List.of("--replicaof");
    public static String REPLICATION_REPLICA_OF = "replicaof";
}