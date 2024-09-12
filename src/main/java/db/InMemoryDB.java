package db;

import db.Database;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryDB implements Database {
    private static volatile InMemoryDB instance = null;
    private InMemoryDB() {
    }
    public static InMemoryDB getInstance() {
        if (instance == null) {
            synchronized (InMemoryDB.class) {
                if (instance == null) {
                    instance = new InMemoryDB();
                }
            }
        }
        return instance;
    }
    private final ConcurrentHashMap<String, CacheEntry> map = new ConcurrentHashMap<>();
    @Override
    public String set(String key, String value) {
        System.out.println(String.format("saving %s value for key %s", value, key));
        map.put(key, CacheEntry.withoutTTL(value));
        System.out.println(map);
        return "+OK\r\n";
    }

    @Override
    public String set(List<String> cmdArgs) {
        CacheEntry cacheEntry;
        if(cmdArgs.get(3).equalsIgnoreCase("FA")){
            cacheEntry = CacheEntry.withExpirationTime(cmdArgs.get(2),Long.parseLong(cmdArgs.get(4)),true);
        }else if(cmdArgs.get(3).equalsIgnoreCase("PX")){
            cacheEntry = CacheEntry.withTTL(cmdArgs.get(2),Long.parseLong(cmdArgs.get(4)),true);
        }else{
            System.out.println("Method not implement for current set opCode");
            throw new IllegalStateException("Method not implement for current set opCode");
        }
        map.put(cmdArgs.get(1), cacheEntry);
        System.out.println("key = " + cmdArgs.get(1) + " CacheEntry = " + cacheEntry);
        System.out.println(map);
        return "+OK\r\n";
    }

    @Override
    public String get(String key) {
        String value;
        if (map.containsKey(key)) {
            CacheEntry cacheEntry = map.get(key);
            value = cacheEntry.getValue();
            if (value == null) {
                map.remove(key);
                return "$-1\r\n";
            }
            System.out.println("value = " + value);
            return String.format("$%d\r\n%s\r\n", value.length(), value);
        }
        return "$-1\r\n";
    }
    @Override
    public String del(String key) {
        if (map.containsKey(key)) {
            map.remove(key);
            return ":1\r\n";
        }
        return ":0\r\n";
    }
    @Override
    public List<String> getKeys() {
        Set<String> keySet = map.keySet();
        return new ArrayList<>(keySet);
    }
}
