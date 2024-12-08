package redis;

public class RedisExecuteException extends RuntimeException {
    public RedisExecuteException(String message) {
        super(message);
    }
}