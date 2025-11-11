import java.util.List;

/**
 * least connections load balancing algorithm
 * selects the backend server with the fewest active connections
 */
public class LeastConnectionsAlgorithm implements LoadBalancingAlgorithm {
    
    /**
     * selects the server with the least number of active connections
     * @param servers the list of available backend servers
     * @return the selected backend server with fewest connections
     */
    @Override
    public BackendServer selectServer(List<BackendServer> servers) {
        if (servers == null || servers.isEmpty()) {
            return null;
        }
        
        // find the server with the minimum number of active connections
        BackendServer selected = servers.get(0);
        int minConnections = selected.getActiveConnections();
        
        // loop through all servers to find the one with least connections
        for (BackendServer server : servers) {
            int connections = server.getActiveConnections();
            if (connections < minConnections) {
                minConnections = connections;
                selected = server;
            }
        }
        
        return selected;
    }
}

