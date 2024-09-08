package utils;
import java.util.List;
public class RedisResponseBuilder {
    public static String responseBuilder(List<String> args){
        String res = String.format("*%d\r\n",args.size());
        for(int i=0;i<args.size();i++){
            res+=wrapper(args.get(i));
        }
        System.out.println(res);
        return res;
    }
    private static String wrapper(String raw){
        return String.format("$%s\r\n%s\r\n",raw.length(),raw);
    }
}