package redis;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import lombok.extern.slf4j.Slf4j;

import static redis.CommonConstant.REDIS_MINIMUM_ENTRY_KEY_Allowed;
import static redis.RedisResultData.getBulkStringData;
import static redis.RedisResultData.getSimpleResultData;

@Slf4j
public class RedisRepository {
    private static final Map<String, String> REDIS_MAP = new HashMap<>();
    private static final Map<String, Long> REDIS_TIMESTAMP_MAP = new HashMap<>();
    private static final Map<String, String> REDIS_CONFIG_MAP = new HashMap<>();
    private static final Map<String, String> REDIS_REPLICATION_INFO_MAP = new HashMap<>();
    private static final Map<String, List<String>> REDIS_REPLICATION_CONFIG_MAP = new HashMap<>();
    private static final Map<String, RedisDataType> REDIS_TYPE_MAP = new HashMap<>();
    private static final Map<String, TreeMap<String, Map<String, String>>> REDIS_STREAM_MAP = new HashMap<>();

    //TODO change the TREEMAP<String,Map<String,String> to TreeMAP<Entry,MAP<String,String>> Entry will have seperate t
    //TODO time and sequence number
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

    public static String getType(String key){
        //Checking Stream by Stream ID
        if (REDIS_STREAM_MAP.containsKey(key)) {
            return RedisDataType.STREAM.toString();
        }
        //Checking if it's in Main inMemory store
        if (REDIS_TYPE_MAP.containsKey(key)) {
            return REDIS_TYPE_MAP.get(key).toString();
        }

        return null;
    }

    public static List<String> getKeys() {
        return REDIS_MAP.keySet().stream().toList();
    }

    public static String configGet(String key) {
        return REDIS_CONFIG_MAP.getOrDefault(key, null);
    }

    public static void set(String key, String value) {
        REDIS_MAP.put(key, value);
        REDIS_TYPE_MAP.put(key, RedisDataType.STRING);
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

    public static String xadd(String key,String entryId, Map<String, String> fields) {
        //if Stream is not create one
        if (!REDIS_STREAM_MAP.containsKey(key)) {
            REDIS_STREAM_MAP.put(key, new TreeMap<>());
            REDIS_TYPE_MAP.put(key, RedisDataType.STREAM);
        }
        // if entryId contains * we dont's need to do validation
        if(entryId.contains("*")) {
            //Generate entryID
            //TWO condition and "time-*" and "*"
            if(entryId.equals("*")) {
                //Generate time Sequence as well as time and sequenceId
                entryId = generateFullStreamId();
            }else{
                entryId = generatePartialStreamId( key,entryId);
            }
        }
        else{
            //Do the validation
            if (entryId.compareTo(REDIS_MINIMUM_ENTRY_KEY_Allowed) < 0) {
                throw new RedisException("The ID specified in XADD must be greater than " + entryId);
            }
            TreeMap<String, Map<String, String>> entryMap = REDIS_STREAM_MAP.get(key);
            if(entryMap.containsKey(entryId) || (!entryMap.isEmpty() && entryMap.lastKey().compareTo(entryId) > 0)) {
                throw new RedisException("The ID specified in XADD is equal or smaller than the target stream top item");
            }
        }
        REDIS_STREAM_MAP.get(key).put(entryId, fields);
        return entryId;
    }

    private static String generatePartialStreamId(String key,String entryId) {
        TreeMap<String, Map<String, String>> entryMap = REDIS_STREAM_MAP.get(key);
        //Get last sequence of entry
        String[] elements = entryId.split("-");
        String time = elements[0];
        String lastSequence = "-1";
        if (!entryMap.isEmpty()) {
            // Get all entries that start with this time
            String lastMatchingKey = entryMap.floorKey(time + "-\uffff");
            if (lastMatchingKey != null && lastMatchingKey.startsWith(time + "-")) {
                // Extract the sequence number
                String[] lastElements = lastMatchingKey.split("-");
                lastSequence = lastElements[1];
            }
        }
        if(time.equals("0") && lastSequence.equals("-1")) {
            lastSequence = "0";
        }
        int nextSequence = Integer.parseInt(lastSequence) + 1;
        entryId = time + "-" + nextSequence;
        log.info("Generated Stream entry id: {}", entryId);
        return entryId;
    }

    private static String generateFullStreamId() {
        return String.format("%d-0", System.currentTimeMillis());
    }

    public static List<XRangeEntry> getEntriesInRange(String steamKey, String start, String end) {
        TreeMap<String, Map<String, String>> entryMap = REDIS_STREAM_MAP.get(steamKey);
        if (entryMap == null || entryMap.isEmpty()) {
            throw new RedisException("Stream does not exist");
        }
        // Handle special start/end cases
        String effectiveStart = start.equals("-") ? entryMap.firstKey() : start;
        String effectiveEnd = end.equals("+") ? entryMap.lastKey() : end;

        NavigableMap<String, Map<String, String>> rangeMap = entryMap.subMap(
                effectiveStart, true,
                effectiveEnd, true
        );

        return rangeMap.entrySet().stream()
                .map(entry -> new XRangeEntry(
                        entry.getKey(),
                        entry.getValue()
                ))
                .toList();
    }

    public static List<XReadEntry> getXReadEntry(List<String> StreamsKeySequence) {
        Map<String,String> StreamsKeySequenceMap = new LinkedHashMap<>();
        int size = StreamsKeySequence.size();
        for(int i = 0; i < size/2; i++) {
            StreamsKeySequenceMap.put(StreamsKeySequence.get(i),StreamsKeySequence.get(size/2+i));
        }
        log.info("StreamsKeySequenceMap: {}", StreamsKeySequenceMap);
        //this define the stream with timestamps
        List<XReadEntry> ans = new ArrayList<>();
        for (Map.Entry<String, String> entry : StreamsKeySequenceMap.entrySet()) {
            String streamKey = entry.getKey();
            String sequence = entry.getValue();
            TreeMap<String, Map<String, String>> entryMap = REDIS_STREAM_MAP.get(streamKey);
            NavigableMap<String, Map<String, String>> greaterThanSequence = entryMap.tailMap(sequence, false);
            XReadEntry.Builder xReadEntrybuilder = new XReadEntry.Builder();
            XReadEntry.SingleStreamEntry.Builder singleStreamEntrybuilder = new XReadEntry.SingleStreamEntry.Builder();
            for(Map.Entry<String, Map<String, String>> entries : greaterThanSequence.entrySet()) {
                singleStreamEntrybuilder.sequence(entries.getKey());
                for(Map.Entry<String, String> field : entries.getValue().entrySet()) {
                    singleStreamEntrybuilder.addField(field.getKey(), field.getValue());
                }
                XReadEntry.SingleStreamEntry singleStreamEntry = singleStreamEntrybuilder.build();
                ans.add(xReadEntrybuilder.streamKey(streamKey).addEntry(singleStreamEntry).build());
            }
        }
        return ans;
    }

    public static List<XReadEntry> getXReadEntryBlocking(List<String> StreamsKeySequence, Long block) {
        //how Blocks can be used
        //XREAD BLOCK 5000 STREAMS | my_stream $	until | we already extracted
        //XREAD BLOCK 5000 STREAMS | orders payments $ $  until | we already Extracted

//        if(block == 0){//Block indefintely
//
////            if(timestamp == '$'){ only want new entries for this
////     give me newer
////            }
//        }
        Map<String,String> StreamsKeySequenceMap = new LinkedHashMap<>();
        int size = StreamsKeySequence.size();
        for(int i = 0; i < size/2; i++) {
            StreamsKeySequenceMap.put(StreamsKeySequence.get(i),StreamsKeySequence.get(size/2+i));
        }
        log.info("StreamsKeySequenceMap blocking : {}", StreamsKeySequenceMap);
        List<XReadEntry> ans = new ArrayList<>();
        Map<String,Integer> streamKeyToMetaData = new LinkedHashMap<>();
        Map<String,String> streamKeyToMetaDataLastEntry = new LinkedHashMap<>();
        for(Map.Entry<String, String> entry : StreamsKeySequenceMap.entrySet()) {
            String streamKey = entry.getKey();
            TreeMap<String, Map<String, String>> entryMap = REDIS_STREAM_MAP.get(streamKey);
            streamKeyToMetaData.put(streamKey,entryMap.size());
            streamKeyToMetaDataLastEntry.put(streamKey,entryMap.lastKey());
        }
        long startTime = System.currentTimeMillis();
        do {
            for (Map.Entry<String, String> Stream : StreamsKeySequenceMap.entrySet()) {
                String streamKey = Stream.getKey();
                String sequence = Stream.getValue();
                TreeMap<String, Map<String, String>> entryMap = REDIS_STREAM_MAP.get(streamKey);
                NavigableMap<String, Map<String, String>> greaterThanSequence;
                XReadEntry.Builder xReadEntrybuilder = new XReadEntry.Builder();

                if(sequence.equals("$")) {
                    Integer originalSize = streamKeyToMetaData.get(streamKey);
                    if(entryMap.size() > originalSize) {
                        String lastSequence = streamKeyToMetaDataLastEntry.get(streamKey);
                        greaterThanSequence = entryMap.tailMap(lastSequence, false);
                        XReadEntry.SingleStreamEntry.Builder singleStreamEntrybuilder = new XReadEntry.SingleStreamEntry.Builder();

                        for (Map.Entry<String, Map<String, String>> entries : greaterThanSequence.entrySet()) {
                            singleStreamEntrybuilder.sequence(entries.getKey());
                            for (Map.Entry<String, String> field : entries.getValue().entrySet()) {
                                singleStreamEntrybuilder.addField(field.getKey(), field.getValue());
                            }
                            XReadEntry.SingleStreamEntry singleStreamEntry = singleStreamEntrybuilder.build();
                            ans.add(xReadEntrybuilder.streamKey(streamKey).addEntry(singleStreamEntry).build());
                        }

                    }
                }else {
                    greaterThanSequence = entryMap.tailMap(sequence, false);
                    XReadEntry.SingleStreamEntry.Builder singleStreamEntrybuilder = new XReadEntry.SingleStreamEntry.Builder();
                    for (Map.Entry<String, Map<String, String>> entries : greaterThanSequence.entrySet()) {
                        singleStreamEntrybuilder.sequence(entries.getKey());
                        for (Map.Entry<String, String> field : entries.getValue().entrySet()) {
                            singleStreamEntrybuilder.addField(field.getKey(), field.getValue());
                        }
                        XReadEntry.SingleStreamEntry singleStreamEntry = singleStreamEntrybuilder.build();
                        ans.add(xReadEntrybuilder.streamKey(streamKey).addEntry(singleStreamEntry).build());
                    }
                }
            }
            if (!ans.isEmpty()) {
                return ans;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return ans;
            }
        } while (block == 0 || System.currentTimeMillis() < startTime + block);

        return ans;
    }
}