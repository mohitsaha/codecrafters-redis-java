package config;

public class RedisConfig {
    private String directory;
    private String dbFilename;
    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public String getDbFilename() {
        return dbFilename;
    }

    public void setDbFilename(String dbFilename) {
        this.dbFilename = dbFilename;
    }
}
