package redis;

public enum RedisDataType {
    NONE("none"),
    STRING("string"),
    LIST("list"),
    SET("set"),
    ZSET("zset"),
    HASH("hash"),
    STREAM("stream");

    private final String value;

    RedisDataType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}