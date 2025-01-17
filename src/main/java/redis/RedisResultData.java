package redis;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public record RedisResultData(
        RespType respType,
        String data
) {
    public static List<RedisResultData> getSimpleResultData(RespType respType, String data) {
        return List.of(new RedisResultData(respType, data));
    }

    public static List<RedisResultData> getArrayDataFromXrange(List<XRangeEntry> xrangeEntries) {
        var result = new ArrayList<RedisResultData>();
        result.add(new RedisResultData(RespType.ARRAYS, String.valueOf(xrangeEntries.size())));
        for(var entry : xrangeEntries){
            String entryID = entry.getEntryId();
            List<String> fields = entry.getFields();
            result.add(new RedisResultData(RespType.ARRAYS, String.valueOf(2)));
            result.addAll(getBulkStringData(entryID));
            result.addAll(getArrayData(fields.toArray(new String[0])));
        }
        return result;
    }

    public static List<RedisResultData> getBulkStringData(String data) {
        var sizeData = new RedisResultData(RespType.BULK_STRINGS, data == null ? "-1" : String.valueOf(data.length()));
        var strData = new RedisResultData(RespType.EMPTY_TYPE, data);

        return data == null ? List.of(sizeData) : List.of(sizeData, strData);
    }

    public static List<RedisResultData> getArrayData(String... args) {
        var result = new ArrayList<RedisResultData>();

        // array size data
        result.add(new RedisResultData(RespType.ARRAYS, String.valueOf(args.length)));
        for (var str : args) {
            result.addAll(getBulkStringData(str));
        }
        return result;
    }

    public static String convertToOutputString(List<RedisResultData> resultDataList) {
        var result = new StringBuilder();

        for (var redisResultData : resultDataList) {
            log.info("checking value  " + redisResultData);
            result.append(redisResultData.respType().getFirstByte());
            result.append(redisResultData.data());
            if (redisResultData.respType.isTrailing()) {
                result.append("\r\n");
            }
        }

        return result.toString();
    }
}