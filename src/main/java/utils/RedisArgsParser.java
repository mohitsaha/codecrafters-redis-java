package utils;

import config.RedisConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class RedisArgsParser {
    public static RedisConfig parseArgs(String[] args) {
        if(args.length ==0){
            return null;
        }
        RedisConfig config = new RedisConfig();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--dir":
                    if (i + 1 < args.length) {
                        config.setDirectory(args[++i]);
                    } else {
                        throw new IllegalArgumentException("Missing argument for --dir");
                    }
                    break;
                case "--dbfilename":
                    if (i + 1 < args.length) {
                        config.setDbFilename(args[++i]);
                    } else {
                        throw new IllegalArgumentException("Missing argument for --dbfilename");
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown argument: " + args[i]);
            }
        }
        return config;
    }

}
