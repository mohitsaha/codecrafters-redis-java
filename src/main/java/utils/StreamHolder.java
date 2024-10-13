package utils;

import java.io.InputStream;
import java.io.PrintStream;

public class StreamHolder {
    public static final ThreadLocal<PrintStream> outputStream = new ThreadLocal<>();
    public static final ThreadLocal<InputStream> inputStream = new ThreadLocal<>();
}