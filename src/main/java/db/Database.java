package db;

import java.util.List;

public interface Database {
    String set(String key, String value);
    String set(List<String> list);
    // Method to get a value by key
    String get(String key);
    String del(String key);
    List<String> getKeys();
}
