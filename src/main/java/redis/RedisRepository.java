package redis;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RedisRepository {
    private static final Map<String, String> REDIS_MAP = new HashMap<>();
    private static final Map<String, Long> REDIS_TIMESTAMP_MAP = new HashMap<>();
    private static final Map<String, String> REDIS_CONFIG_MAP = new HashMap<>();
    private static final Map<String, String> REDIS_REPLICATION_INFO_MAP = new HashMap<>();
    private static final Map<String, List<String>> REDIS_REPLICATION_CONFIG_MAP = new HashMap<>();

    private RedisRepository() {

    }

    public static String get(String key) {
        if (REDIS_TIMESTAMP_MAP.containsKey(key)) {
            var expireTimestamp = REDIS_TIMESTAMP_MAP.get(key);

            if (Instant.ofEpochMilli(expireTimestamp).isBefore(Instant.now())) {
                REDIS_TIMESTAMP_MAP.remove(key);
                REDIS_MAP.remove(key);
                return null;
            }
        }

        return REDIS_MAP.getOrDefault(key, null);
    }

    public static List<String> getKeys() {
        return REDIS_MAP.keySet().stream().toList();
    }

    public static String configGet(String key) {
        return REDIS_CONFIG_MAP.getOrDefault(key, null);
    }

    public static void set(String key, String value) {
        REDIS_MAP.put(key, value);
    }

    public static void del(String key) {
        REDIS_MAP.remove(key);
    }

    public static void setWithExpireTimestamp(String key, String value, long timeStamp) {
        REDIS_MAP.put(key, value);
        REDIS_TIMESTAMP_MAP.put(key, timeStamp);
    }

    public static void configSet(String key, String value) {
        REDIS_CONFIG_MAP.put(key, value);
    }

    public static void expireWithExpireTime(String key, long expireTime) {
        var instant = Instant.now();
        var expireTimeStamp = instant.plus(expireTime, ChronoUnit.MILLIS);

        REDIS_TIMESTAMP_MAP.put(key, expireTimeStamp.toEpochMilli());
    }

    public static void expire(String key) {
        REDIS_MAP.remove(key);
    }

    public static void setReplicationSetting(String key, String value) {
        REDIS_REPLICATION_INFO_MAP.put(key, value);
    }

    public static String getReplicationSetting(String key) {
        return REDIS_REPLICATION_INFO_MAP.getOrDefault(key, null);
    }

    public static String getReplicationSetting(String key, String defaultValue) {
        return REDIS_REPLICATION_INFO_MAP.getOrDefault(key, defaultValue);
    }

    public static List<Map.Entry<String, String>> getAllReplicationSettings() {
        return REDIS_REPLICATION_INFO_MAP.keySet().stream()
                .map(key -> Map.entry(key, REDIS_REPLICATION_INFO_MAP.get(key)))
                .toList();
    }

    public static void setReplicationConfig(String key, List<String> values) {
        REDIS_REPLICATION_CONFIG_MAP.put(key, values);
    }

    public static List<String> getReplicationConfig(String key) {
        return REDIS_REPLICATION_CONFIG_MAP.getOrDefault(key, null);
    }
}