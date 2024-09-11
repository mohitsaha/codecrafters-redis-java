package db;

import config.RedisConfig;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

public class RDBFile {
    private RedisConfig config = null;
    public static final byte OPCODE_END_OF_FILE = (byte) 0xFF;
    public static final byte OPCODE_DATABASE_SELECTOR = (byte) 0xFE;
    public static final byte OPCODE_EXPIRE_TIME = (byte) 0xFD;
    public static final byte OPCODE_EXPIRE_TIME_MILLISECONDS = (byte) 0xFC;
    public static final byte OPCODE_RESIZE_DATABASE = (byte) 0xFB;
    public static final byte OPCODE_AUXILIARY_FIELDS = (byte) 0xFA;
    public static final byte LENGTH_6BIT = 0b00;
    public static final byte LENGTH_14BIT = 0b01;
    public static final byte LENGTH_32BIT = 0b10;
    public static final byte LENGTH_SPECIAL = 0b11;
    public static final byte STRING_INTEGER_8BIT = 0;
    public static final byte STRING_INTEGER_16BIT = 1;
    public static final byte STRING_INTEGER_32BIT = 2;
    public static final byte STRING_VALUE_TYPE = 0;
    Database db = InMemoryDB.getInstance();
    public RDBFile(RedisConfig config) {
        this.config = config;
        CreateRDBFile(config);
        loadRDBFileInMemory();
    }
    private void loadRDBFileInMemory() {
        try {
            File file = new File(config.getDirectory(), config.getDbFilename());
            FileInputStream fis = new FileInputStream(file);
            DataInputStream dataInputStream = new DataInputStream(fis);
            parseMagic(dataInputStream);
            final var version = parseVersion(dataInputStream);
            log("version", version);
            Integer databaseNumber = null;
            while (true) {
                final var opcode = dataInputStream.readByte();
                if (opcode == OPCODE_END_OF_FILE) {
                    log("end of file");
                    break;
                }
                switch (opcode) {
                    case OPCODE_AUXILIARY_FIELDS: {
                        final var key = readString(dataInputStream);
                        final var value = readString(dataInputStream);
                        log("metadata", key, value);
                        break;
                    }
                    case OPCODE_DATABASE_SELECTOR: {
                        databaseNumber = readUnsignedByte(dataInputStream);
                        log("databaseNumber", databaseNumber);
                        break;
                    }
                    case OPCODE_RESIZE_DATABASE: {
                        final var hashTableSize = readLength(dataInputStream);
                        log("hashTableSize", hashTableSize);
                        final var expireHashTableSize = readLength(dataInputStream);
                        log("expireHashTableSize", expireHashTableSize);
                        break;
                    }
                    case OPCODE_EXPIRE_TIME: {
                        throw new UnsupportedOperationException(
                                "unsupported OPCODE_EXPIRE_TIME");
                    }
                    case OPCODE_EXPIRE_TIME_MILLISECONDS:
                    default: {
                        if (databaseNumber == null) {
                            throw new IllegalArgumentException(
                                    "unexpected value: %x".formatted(opcode));
                        }
                        int valueType = opcode;
                        long expiration = -1;
                        if (opcode == OPCODE_EXPIRE_TIME_MILLISECONDS) {
                            expiration = readUnsignedLong(dataInputStream);
                            valueType = readUnsignedByte(dataInputStream);
                            log("expiration", expiration);
                        }
                        final var key = readString(dataInputStream);
                        log("key", key);
                        final var value = readValue(dataInputStream,valueType);
                        log("value", value);
                        if (expiration == -1) {
                            db.set(key, (String) value);
                        } else {
                            throw new IllegalStateException("with expiration not implemented yet");
                        }
                    }
                }
            }
        }catch(Exception e){
            System.out.println("cannot able to parse RDB File");
        }
    }

    private void CreateRDBFile(RedisConfig config) {
        Path dir = config.getDirectory() == null ? Path.of("") : Path.of(config.getDirectory());
        String filename = config.getDbFilename() == null ? "test.rdb" : config.getDbFilename();
        File dbfile = new File(dir.resolve(filename).toString());
        try {
            boolean created = dbfile.createNewFile();
            if (created) {
                System.out.println("File created");
            } else {
                System.out.println("File was already created");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Object readValue(DataInputStream inputStream,int valueType) throws IOException {
        return switch (valueType) {
            case STRING_VALUE_TYPE -> readString(inputStream);
            default -> throw new IllegalStateException("unsupported value type: " + valueType);
        };
    }

    public void parseMagic(DataInputStream inputStream) throws IOException {
        final var magic = new String(inputStream.readNBytes(5), StandardCharsets.US_ASCII);
        if (!magic.equals("REDIS")) {
            throw new IllegalStateException("invalid magic: " + magic);
        }
    }

    public int parseVersion(DataInputStream inputStream) throws IOException {
        final var version = new String(inputStream.readNBytes(4), StandardCharsets.US_ASCII);
        try {
            return Integer.parseInt(version);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("invalid version: " + version, exception);
        }
    }

    public void log(Object... content) {
        System.out.println("rdb: " + Arrays.toString(content));
    }

    public int readUnsignedByte(DataInputStream inputStream) throws IOException {
        return Byte.toUnsignedInt(inputStream.readByte());
    }

    public long readUnsignedInteger(DataInputStream inputStream) throws IOException {
        return Integer.toUnsignedLong(Integer.reverseBytes(inputStream.readInt()));
    }

    public long readUnsignedLong(DataInputStream inputStream) throws IOException {
        return Long.reverseBytes(inputStream.readLong());
    }

    public int readLength(DataInputStream dataInputStream) throws IOException {
        final var first = readUnsignedByte(dataInputStream);
        final var encoding = first >> 6;
        final var value = first & 0b0011_1111;
        return switch (encoding) {
            case LENGTH_6BIT -> value;
            case LENGTH_14BIT -> {
                final var second = readUnsignedByte(dataInputStream);
                yield (value << 8) | second;
            }
            /* bad special "number" encoding */
            case LENGTH_SPECIAL -> -(value + 1);
            default ->
                    throw new IllegalStateException("unexpected length encoding: " + Integer.toBinaryString(encoding));
        };
    }

    public String readString(DataInputStream dataInputStream) throws IOException {
        final var length = readLength(dataInputStream);
        if (length < 0) {
            /* bad special "number" encoding */
            final var type = (-length) - 1;
            // TODO ByteOrder?
            return switch (type) {
                case STRING_INTEGER_8BIT -> String.valueOf(Byte.toUnsignedInt(dataInputStream.readByte()));
                case STRING_INTEGER_16BIT ->
                        String.valueOf(Short.toUnsignedInt(Short.reverseBytes(dataInputStream.readShort())));
                case STRING_INTEGER_32BIT -> Integer.toUnsignedString(Integer.reverseBytes(dataInputStream.readInt()));
                default ->
                        throw new IllegalArgumentException("unexpected length type: " + Integer.toBinaryString(type));
            };
        }
        final var content = dataInputStream.readNBytes(length);
        return new String(content, StandardCharsets.US_ASCII);
    }

    public void skip(DataInputStream inputStream,int length) throws IOException {
        inputStream.skip(length);
    }

}