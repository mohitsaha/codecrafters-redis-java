package utils;

import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;

public class StreamHolder {
    public static final ThreadLocal<PrintStream> outputStream = new ThreadLocal<>();
    public static final ThreadLocal<InputStream> inputStream = new ThreadLocal<>();
    public static final ThreadLocal<Socket> socket = new ThreadLocal<>();
}