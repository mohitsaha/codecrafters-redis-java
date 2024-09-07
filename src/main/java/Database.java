public interface Database {
    String set(String key, String value);

    // Method to get a value by key
    String get(String key);

    // Method to delete a key
    String del(String key);
}
