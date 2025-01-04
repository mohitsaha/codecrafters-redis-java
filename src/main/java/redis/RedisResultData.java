package redis;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public record RedisResultData(
        RedisDataType redisDataType,
        String data
) {
    public static List<RedisResultData> getSimpleResultData(RedisDataType redisDataType, String data) {
        return List.of(new RedisResultData(redisDataType, data));
    }

    public static List<RedisResultData> getBulkStringData(String data) {
        var sizeData = new RedisResultData(RedisDataType.BULK_STRINGS, data == null ? "-1" : String.valueOf(data.length()));
        var strData = new RedisResultData(RedisDataType.EMPTY_TYPE, data);

        return data == null ? List.of(sizeData) : List.of(sizeData, strData);
    }

    public static List<RedisResultData> getArrayData(String... args) {
        var result = new ArrayList<RedisResultData>();

        // array size data
        result.add(new RedisResultData(RedisDataType.ARRAYS, String.valueOf(args.length)));
        for (var str : args) {
            result.addAll(getBulkStringData(str));
        }

        return result;
    }

    public static String convertToOutputString(List<RedisResultData> resultDataList) {
        var result = new StringBuilder();

        for (var redisResultData : resultDataList) {
            log.info("checking value  " + redisResultData);
            result.append(redisResultData.redisDataType().getFirstByte());
            result.append(redisResultData.data());
            if (redisResultData.redisDataType.isTrailing()) {
                result.append("\r\n");
            }
        }

        return result.toString();
    }
}