package replication;

public record ReplicationConnectionInfo(
        String ipAddress,
        int connectionPort
) {
}
