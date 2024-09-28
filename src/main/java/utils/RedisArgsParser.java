package utils;

import config.RedisConfig;

public class RedisArgsParser {
    public static RedisConfig parseArgs(String[] args) {
        if(args.length ==0){
            return null;
        }
        RedisConfig.Builder ConfigBuilder = new RedisConfig.Builder();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--dir":
                    if (i + 1 < args.length) {
                        ConfigBuilder.setDirectory(args[++i]);
                    } else {
                        throw new IllegalArgumentException("Missing argument for --dir");
                    }
                    break;
                case "--dbfilename":
                    if (i + 1 < args.length) {
                        ConfigBuilder.setDbFilename(args[++i]);
                    } else {
                        throw new IllegalArgumentException("Missing argument for --dbfilename");
                    }
                    break;
                case "--port":
                    if (i + 1 < args.length) {
                        ConfigBuilder.setPortNumber(Integer.parseInt(args[++i]));
                    } else {
                        throw new IllegalArgumentException("Missing argument for --port");
                    }
                    break;
                case "--replicaof":
                    if (i + 1 < args.length) {
                        ConfigBuilder.setReplicaOffHost(args[++i]);
                    } else {
                        throw new IllegalArgumentException("Missing argument for --replicaof");
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown argument: " + args[i]);
            }
        }
        return ConfigBuilder.build();
    }

}
