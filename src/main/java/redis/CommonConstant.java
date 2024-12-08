package redis;

public class CommonConstant {
    private CommonConstant() {
    }

    public static final int DEFAULT_REDIS_PORT = 6379;

    public static final String REDIS_OUTPUT_PONG = "PONG";
    public static final String REDIS_OUTPUT_OK = "OK";
    public static final String REDIS_COMMAND_PARAM_GET = "GET";
}