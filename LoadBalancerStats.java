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
    
    // load monitoring metrics
    private long startTime;
    private long lastRequestTime;
    private long requestsInLastMinute;
    private long lastMinuteStartTime;
    
    /**
     * constructor - initializes all counters to zero
     */
    public LoadBalancerStats() {
        this.totalRequests = 0;
        this.successfulRequests = 0;
        this.failedRequests = 0;
        this.totalConnections = 0;
        this.startTime = System.currentTimeMillis();
        this.lastRequestTime = 0;
        this.requestsInLastMinute = 0;
        this.lastMinuteStartTime = System.currentTimeMillis();
    }
    
    /**
     * increments the total request count
     */
    public synchronized void incrementTotalRequests() {
        totalRequests++;
        lastRequestTime = System.currentTimeMillis();
        
        // update requests in last minute
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMinuteStartTime > 60000) {
            // reset if more than a minute has passed
            requestsInLastMinute = 1;
            lastMinuteStartTime = currentTime;
        } else {
            requestsInLastMinute++;
        }
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
        
        // load monitoring metrics
        long uptime = getUptimeSeconds();
        System.out.println("Uptime: " + uptime + " seconds");
        System.out.println("Requests/Second: " + String.format("%.2f", getRequestsPerSecond()));
        System.out.println("Requests/Minute: " + String.format("%.2f", getRequestsPerMinute()));
        
        long sinceLast = getSecondsSinceLastRequest();
        if (sinceLast >= 0) {
            System.out.println("Seconds Since Last Request: " + sinceLast);
        }
        
        System.out.println("\nBackend Server Status:");
        for (BackendServer server : backendServers) {
            String status = server.isHealthy() ? "healthy" : "unhealthy";
            System.out.println("  " + server.getHost() + ":" + server.getPort() + 
                             " - " + status + 
                             " (connections: " + server.getActiveConnections() + 
                             ", requests: " + server.getTotalRequestsHandled() +
                             ", load: " + String.format("%.1f", server.getLoadPercentage()) + "%)");
        }
        System.out.println("================================\n");
    }
    
    /**
     * gets the uptime in seconds
     * @return uptime in seconds
     */
    public long getUptimeSeconds() {
        return (System.currentTimeMillis() - startTime) / 1000;
    }
    
    /**
     * gets the requests per second (average over uptime)
     * @return requests per second
     */
    public double getRequestsPerSecond() {
        long uptime = getUptimeSeconds();
        if (uptime == 0) {
            return 0.0;
        }
        return (double) totalRequests / uptime;
    }
    
    /**
     * gets the requests per minute (based on last minute)
     * @return requests per minute
     */
    public synchronized double getRequestsPerMinute() {
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastMinuteStartTime;
        
        if (elapsed == 0) {
            return 0.0;
        }
        
        // calculate rate based on actual elapsed time
        double rate = (requestsInLastMinute * 60000.0) / elapsed;
        return rate;
    }
    
    /**
     * gets the time since last request in seconds
     * @return seconds since last request, or -1 if no requests
     */
    public long getSecondsSinceLastRequest() {
        if (lastRequestTime == 0) {
            return -1;
        }
        return (System.currentTimeMillis() - lastRequestTime) / 1000;
    }
    
    /**
     * resets all statistics to zero
     */
    public synchronized void reset() {
        totalRequests = 0;
        successfulRequests = 0;
        failedRequests = 0;
        totalConnections = 0;
        startTime = System.currentTimeMillis();
        lastRequestTime = 0;
        requestsInLastMinute = 0;
        lastMinuteStartTime = System.currentTimeMillis();
    }
}

