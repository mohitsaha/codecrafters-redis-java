package utils;

import config.RedisConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class RedisArgsParser {
    public static RedisConfig parseArgs(String[] args) {
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
        CreateRDBFile(config);
        return config;
    }
    private static void CreateRDBFile(RedisConfig config){
        Path dir =  config.getDirectory() == null ? Path.of(""): Path.of(config.getDirectory());
        String filename = config.getDbFilename() == null ? "test.rdb" : config.getDbFilename();
        File dbfile = new File(dir.resolve(filename).toString());
        try {
            boolean created = dbfile.createNewFile();
            if(created){
                System.out.println("File created");
            }else{
                System.out.println("File was already created");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
