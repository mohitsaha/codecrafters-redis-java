package db;

public class CacheEntry {
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
    private CacheEntry(String value, long expirationTime, long ttl, boolean expiringEntry) {
        this.value = value;
        this.expirationTime = expirationTime;
        this.ttl = ttl;
        this.expiringEntry = expiringEntry;
    }
    public static CacheEntry withTTL(String value, long ttl, boolean expiringEntry) {
        long expirationTime = System.currentTimeMillis() + ttl;
        return new CacheEntry(value, expirationTime, ttl, expiringEntry);
    }
    public static CacheEntry withExpirationTime(String value, long expirationTime, boolean expiringEntry) {
        return new CacheEntry(value, expirationTime, 0, expiringEntry);
    }

    public static CacheEntry withoutTTL(String value){
        return new CacheEntry(value,0,0,false);
    }
    public String getValue() {
        if (!expiringEntry) {
            return value;
        } else if (System.currentTimeMillis() > expirationTime) {
            return null;
        } else {
            return value;
        }
    }
    public long getExpirationTime() {
        return expirationTime;
    }

    public long getTtl() {
        return ttl;
    }

    public boolean isExpiringEntry() {
        return expiringEntry;
    }
}
