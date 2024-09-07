import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryDB implements Database {
    private final ConcurrentHashMap<String,CacheEntry> map = new ConcurrentHashMap<>();
    @Override
    public String set(String key, String value) {
        System.out.println(String.format("saving %s value for key %s",value,key));
        map.put(key,new CacheEntry(value));
        System.out.println(map);
        return "+OK\r\n";
    }

    @Override
    public String set(List<String> cmdArgs) {
        CacheEntry cacheEntry = new CacheEntry(cmdArgs.get(2),Long.parseLong(cmdArgs.get(4)),true);
        map.put(cmdArgs.get(1), cacheEntry);
        System.out.println("key = " + cmdArgs.get(1) + "CacheEntry = " + cacheEntry);
        System.out.println(map);
        return "+OK\r\n";
    }


    @Override
    public String get(String key) {
        String value;
        if(map.containsKey(key)){
            CacheEntry cacheEntry = map.get(key);
            value = cacheEntry.getValue();
            if(value == null){
                map.remove(key);
                return "$-1\r\n";
            }
            System.out.println("value = " + value);
            return String.format("$%d\r\n%s\r\n",value.length(),value);
        }
        return "$-1\r\n";
    }

    @Override
    public String del(String key) {
        return null;
    }
    class CacheEntry{
        private String value;
        private long expirationTime;
        private long ttl;
        private boolean expiringEntry = false;

        @Override
        public String toString() {
            return "CacheEntry{" +
                    "value='" + value + '\'' +
                    ", expirationTime=" + expirationTime +
                    ", ttl=" + ttl +
                    ", expiringEntry=" + expiringEntry +
                    '}';
        }

        public CacheEntry(String value){
            this.value = value;
        }
        public CacheEntry(String value, long ttl, boolean expiringEntry) {
            this.value = value;
            this.ttl = ttl;
            this.expirationTime = System.currentTimeMillis() + ttl;
            this.expiringEntry = expiringEntry;
        }

        public String getValue() {
            if(!expiringEntry)
               return value;
            else
                if(System.currentTimeMillis() > expirationTime){
                    return null;
                }else{
                    return value;
                }
        }
    }
}
