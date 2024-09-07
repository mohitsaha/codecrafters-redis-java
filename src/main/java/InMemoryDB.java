import java.util.HashMap;

public class InMemoryDB implements Database {
    private final HashMap<String,String> map = new HashMap<>();
    @Override
    public String set(String key, String value) {
        System.out.println(String.format("saving %s value for key %s",value,key));
        map.put(key,value);
        return "+OK\r\n";
    }

    @Override
    public String get(String key) {
        String value;
        if(map.containsKey(key)){
            value = map.get(key);

            System.out.println("value = " +value);
            return String.format("$%d\r\n%s\r\n",value.length(),value);
        }
        return "$-1\r\n";
    }

    @Override
    public String del(String key) {
        return null;
    }
}
