package utils;

import java.io.PrintStream;

public class OutputStreamHolder {
    public static final ThreadLocal<PrintStream> outputStream = new ThreadLocal<>();
}