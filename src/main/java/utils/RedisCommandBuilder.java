package utils;

import java.util.ArrayList;
import java.util.List;

public class RedisCommandBuilder {
    private List<String> args;

    public RedisCommandBuilder() {
        this.args = new ArrayList<>();
    }

    public RedisCommandBuilder command(String command) {
        if (command == null || command.isEmpty()) {
            throw new IllegalArgumentException("Command cannot be null or empty.");
        }
        args.add(command);
        return this;
    }
    public RedisCommandBuilder argument(String arg) {
        if (arg == null || arg.isEmpty()) {
            throw new IllegalArgumentException("Argument cannot be null or empty.");
        }
        args.add(arg);
        return this;
    }

    public RedisCommandBuilder argument(int number) {
        args.add(Integer.toString(number));
        return this;
    }
    public String build() {
        if (args.isEmpty()) {
            throw new IllegalStateException("No command provided.");
        }
        String response =  RedisResponseBuilder.responseBuilder(args);
        args.clear();
        return response;
    }
}
