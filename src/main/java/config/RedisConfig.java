package config;

public class RedisConfig {
    private String directory;
    private String dbFilename;
    private int portNumber;
    private String replicaOffHost;
    private RedisConfig() {}
    public static class Builder {
        private String directory;
        private String dbFilename;
        private int portNumber;
        private String replicaOffHost;
        public Builder setDirectory(String directory) {
            this.directory = directory;
            return this;
        }

        public Builder setDbFilename(String dbFilename) {
            this.dbFilename = dbFilename;
            return this;
        }

        public Builder setPortNumber(int portNumber) {
            this.portNumber = portNumber;
            return this;
        }

        public Builder setReplicaOffHost(String host){
            this.replicaOffHost = host;
            return this;
        }
        public RedisConfig build() {
            RedisConfig config = new RedisConfig();
            config.directory = this.directory;
            config.dbFilename = this.dbFilename;
            config.portNumber = this.portNumber;
            config.replicaOffHost = this.replicaOffHost;
            return config;
        }
    }
}
