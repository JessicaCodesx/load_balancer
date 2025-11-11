/**
 * represents a backend server that can handle client requests
 */
public class BackendServer {
    private String host;
    private int port;
    private int activeConnections;
    private boolean isHealthy;
    private long lastHealthCheckTime;
    
    /**
     * constructor for a backend server
     * @param host the server hostname or ip
     * @param port the server port
     */
    public BackendServer(String host, int port) {
        this.host = host;
        this.port = port;
        this.activeConnections = 0;
        this.isHealthy = true; // assume healthy initially
        this.lastHealthCheckTime = 0;
    }
    
    public String getHost() {
        return host;
    }
    
    public int getPort() {
        return port;
    }
    
    public int getActiveConnections() {
        return activeConnections;
    }
    
    /**
     * increments the active connection count
     */
    public void incrementConnections() {
        activeConnections++;
    }
    
    /**
     * decrements the active connection count
     */
    public void decrementConnections() {
        if (activeConnections > 0) {
            activeConnections--;
        }
    }
    
    public boolean isHealthy() {
        return isHealthy;
    }
    
    public void setHealthy(boolean healthy) {
        this.isHealthy = healthy;
    }
    
    public long getLastHealthCheckTime() {
        return lastHealthCheckTime;
    }
    
    public void setLastHealthCheckTime(long time) {
        this.lastHealthCheckTime = time;
    }
    
    @Override
    public String toString() {
        String status = isHealthy ? "healthy" : "unhealthy";
        return host + ":" + port + " (" + status + ")";
    }
}

