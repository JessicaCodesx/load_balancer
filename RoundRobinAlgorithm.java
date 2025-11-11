import java.util.List;

/**
 * round-robin load balancing algorithm
 * distributes requests evenly by cycling through servers in order
 */
public class RoundRobinAlgorithm implements LoadBalancingAlgorithm {
    private int currentIndex = 0;
    
    /**
     * selects the next server in round-robin fashion
     * @param servers the list of available backend servers
     * @return the selected backend server
     */
    @Override
    public BackendServer selectServer(List<BackendServer> servers) {
        if (servers == null || servers.isEmpty()) {
            return null;
        }
        
        // get the current server and move to next one
        BackendServer selected = servers.get(currentIndex);
        currentIndex = (currentIndex + 1) % servers.size();
        
        return selected;
    }
}

