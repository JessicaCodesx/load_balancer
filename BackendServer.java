/**
 * represents a backend server that can handle client requests
 */
public class BackendServer {
    private String host;
    private int port;
    private int activeConnections;
    
    /**
     * constructor for a backend server
     * @param host the server hostname or ip
     * @param port the server port
     */
    public BackendServer(String host, int port) {
        this.host = host;
        this.port = port;
        this.activeConnections = 0;
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
    
    @Override
    public String toString() {
        return host + ":" + port;
    }
}

