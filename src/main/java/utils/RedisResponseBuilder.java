package utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RedisResponseBuilder {
    private static final Logger logger = LoggerFactory.getLogger(RedisResponseBuilder.class);
    public static String responseBuilder(List<String> args){
        String res = String.format("*%d\r\n",args.size());
        for(int i=0;i<args.size();i++){
            res+=wrapper(args.get(i));
        }
        logger.info("Response split into lines: {}", Arrays.stream(res.split("\r\n")).collect(Collectors.toList()));
        return res;
    }
    public static String respIntegerBuilder(int num){
        return ":"+num+"\r\n";
    }
    public static String wrapper(String raw){
        return String.format("$%s\r\n%s\r\n",raw.length(),raw);
    }
}