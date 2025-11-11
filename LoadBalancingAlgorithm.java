import java.util.List;

/**
 * interface for load balancing algorithms
 */
public interface LoadBalancingAlgorithm {
    /**
     * selects the next backend server to handle a request
     * @param servers the list of available backend servers
     * @return the selected backend server
     */
    BackendServer selectServer(List<BackendServer> servers);
}

