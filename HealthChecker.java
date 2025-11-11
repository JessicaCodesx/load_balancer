import java.io.IOException;
import java.net.Socket;
import java.util.List;

/**
 * periodically checks the health of backend servers
 * by attempting to connect to them
 */
public class HealthChecker implements Runnable {
    private List<BackendServer> backendServers;
    private int checkIntervalSeconds;
    private int connectionTimeoutMs;
    private volatile boolean running;
    
    /**
     * constructor for the health checker
     * @param backendServers the list of servers to check
     * @param checkIntervalSeconds how often to check (in seconds)
     * @param connectionTimeoutMs timeout for connection attempts (in milliseconds)
     */
    public HealthChecker(List<BackendServer> backendServers, int checkIntervalSeconds, int connectionTimeoutMs) {
        this.backendServers = backendServers;
        this.checkIntervalSeconds = checkIntervalSeconds;
        this.connectionTimeoutMs = connectionTimeoutMs;
        this.running = true;
    }
    
    /**
     * stops the health checker
     */
    public void stop() {
        running = false;
    }
    
    /**
     * main health check loop
     */
    @Override
    public void run() {
        System.out.println("health checker started (checking every " + checkIntervalSeconds + " seconds)");
        
        while (running) {
            // check each backend server
            for (BackendServer server : backendServers) {
                boolean healthy = checkServerHealth(server);
                server.setHealthy(healthy);
                server.setLastHealthCheckTime(System.currentTimeMillis());
                
                if (healthy) {
                    System.out.println("health check: " + server.getHost() + ":" + server.getPort() + " is healthy");
                } else {
                    System.out.println("health check: " + server.getHost() + ":" + server.getPort() + " is unhealthy");
                }
            }
            
            // wait before next check
            try {
                Thread.sleep(checkIntervalSeconds * 1000);
            } catch (InterruptedException e) {
                // interrupted, stop checking
                running = false;
                break;
            }
        }
        
        System.out.println("health checker stopped");
    }
    
    /**
     * checks if a server is healthy by attempting to connect
     * @param server the server to check
     * @return true if server is healthy, false otherwise
     */
    private boolean checkServerHealth(BackendServer server) {
        try (Socket socket = new Socket()) {
            // try to connect with a timeout
            socket.connect(new java.net.InetSocketAddress(server.getHost(), server.getPort()), connectionTimeoutMs);
            return true;
        } catch (IOException e) {
            // connection failed, server is unhealthy
            return false;
        }
    }
}

