package config;

import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

public class RedisConfig {
    private String directory;
    private String dbFilename;
    private Integer portNumber;
    private String replicaOfHost;
    private Role role = Role.MASTER;
    public Role getRole(){
        return role;
    }
    public String getDirectory() {
        return directory;
    }
    public String getDbFilename() {
        return dbFilename;
    }
    public Integer getPortNumber() {
        return portNumber;
    }
    public String getReplicaOfHost() {
        return replicaOfHost;
    }
    private volatile Set<OutputStream> replicas = new HashSet<>();

    public void addReplicaOutputStream(OutputStream outputStream){
        replicas.add(outputStream);
    }
    public Set<OutputStream> getReplicas(){
        return replicas;
    }
    public void setRole(Role role){
        this.role = role;
    }
    private RedisConfig() {}
    public static class Builder {
        private String directory;
        private String dbFilename;
        private Integer portNumber;
        private String replicaOfHost;
        private Role role;
        public Builder setDirectory(String directory) {
            this.directory = directory;
            return this;
        }
        public Builder setRole(Role role){
            this.role = role;
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

        public Builder setReplicaOfHost(String host){
            this.replicaOfHost = host;
            return this;
        }
        public RedisConfig build() {
            RedisConfig config = new RedisConfig();
            config.directory = this.directory;
            config.dbFilename = this.dbFilename;
            config.portNumber = this.portNumber;
            config.replicaOfHost = this.replicaOfHost;
            config.role = this.role;
            return config;
        }
    }
}
