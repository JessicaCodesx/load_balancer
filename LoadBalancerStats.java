import java.util.List;

/**
 * tracks statistics for the load balancer
 * keeps counts of requests, connections, and server metrics
 */
public class LoadBalancerStats {
    private long totalRequests;
    private long successfulRequests;
    private long failedRequests;
    private long totalConnections;
    
    /**
     * constructor - initializes all counters to zero
     */
    public LoadBalancerStats() {
        this.totalRequests = 0;
        this.successfulRequests = 0;
        this.failedRequests = 0;
        this.totalConnections = 0;
    }
    
    /**
     * increments the total request count
     */
    public synchronized void incrementTotalRequests() {
        totalRequests++;
    }
    
    /**
     * increments the successful request count
     */
    public synchronized void incrementSuccessfulRequests() {
        successfulRequests++;
    }
    
    /**
     * increments the failed request count
     */
    public synchronized void incrementFailedRequests() {
        failedRequests++;
    }
    
    /**
     * increments the total connection count
     */
    public synchronized void incrementTotalConnections() {
        totalConnections++;
    }
    
    /**
     * gets the total number of requests processed
     * @return total request count
     */
    public long getTotalRequests() {
        return totalRequests;
    }
    
    /**
     * gets the number of successful requests
     * @return successful request count
     */
    public long getSuccessfulRequests() {
        return successfulRequests;
    }
    
    /**
     * gets the number of failed requests
     * @return failed request count
     */
    public long getFailedRequests() {
        return failedRequests;
    }
    
    /**
     * gets the total number of connections handled
     * @return total connection count
     */
    public long getTotalConnections() {
        return totalConnections;
    }
    
    /**
     * prints current statistics to console
     * @param backendServers list of backend servers to show stats for
     */
    public void printStats(List<BackendServer> backendServers) {
        System.out.println("\n=== Load Balancer Statistics ===");
        System.out.println("Total Requests: " + totalRequests);
        System.out.println("Successful Requests: " + successfulRequests);
        System.out.println("Failed Requests: " + failedRequests);
        System.out.println("Total Connections: " + totalConnections);
        
        if (totalRequests > 0) {
            double successRate = (double) successfulRequests / totalRequests * 100;
            System.out.println("Success Rate: " + String.format("%.2f", successRate) + "%");
        }
        
        System.out.println("\nBackend Server Status:");
        for (BackendServer server : backendServers) {
            String status = server.isHealthy() ? "healthy" : "unhealthy";
            System.out.println("  " + server.getHost() + ":" + server.getPort() + 
                             " - " + status + 
                             " (active connections: " + server.getActiveConnections() + ")");
        }
        System.out.println("================================\n");
    }
    
    /**
     * resets all statistics to zero
     */
    public synchronized void reset() {
        totalRequests = 0;
        successfulRequests = 0;
        failedRequests = 0;
        totalConnections = 0;
    }
}

