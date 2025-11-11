/**
 * represents a backend server that can handle client requests
 */
public class BackendServer {
    private String host;
    private int port;
    private int activeConnections;
    private boolean isHealthy;
    private long lastHealthCheckTime;
    
    // load monitoring per server
    private long totalRequestsHandled;
    private long successfulRequestsHandled;
    private long failedRequestsHandled;
    
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
        this.totalRequestsHandled = 0;
        this.successfulRequestsHandled = 0;
        this.failedRequestsHandled = 0;
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
    
    /**
     * increments the total requests handled by this server
     */
    public synchronized void incrementRequestsHandled() {
        totalRequestsHandled++;
    }
    
    /**
     * increments the successful requests handled by this server
     */
    public synchronized void incrementSuccessfulRequests() {
        successfulRequestsHandled++;
    }
    
    /**
     * increments the failed requests handled by this server
     */
    public synchronized void incrementFailedRequests() {
        failedRequestsHandled++;
    }
    
    public long getTotalRequestsHandled() {
        return totalRequestsHandled;
    }
    
    public long getSuccessfulRequestsHandled() {
        return successfulRequestsHandled;
    }
    
    public long getFailedRequestsHandled() {
        return failedRequestsHandled;
    }
    
    /**
     * gets the load percentage based on active connections
     * assumes max capacity of 100 connections for calculation
     * @return load percentage (0-100)
     */
    public double getLoadPercentage() {
        // simple calculation: active connections / max capacity (assume 100)
        return Math.min(100.0, (activeConnections / 100.0) * 100.0);
    }
    
    @Override
    public String toString() {
        String status = isHealthy ? "healthy" : "unhealthy";
        return host + ":" + port + " (" + status + ")";
    }
}

