import java.io.*;
import java.net.*;
import java.util.*;

/**
 * simple load balancer that distributes client requests to backend servers
 */
public class LoadBalancer {
    
    private int loadBalancerPort;
    private List<BackendServer> backendServers;
    private LoadBalancingAlgorithm algorithm;
    
    /**
     * constructor to initialize the load balancer
     * @param port the port the load balancer will listen on
     */
    public LoadBalancer(int port) {
        this.loadBalancerPort = port;
        this.backendServers = new ArrayList<>();
        // default to round-robin algorithm
        this.algorithm = new RoundRobinAlgorithm();
    }
    
    /**
     * main method to start the load balancer
     */
    public static void main(String[] args) {
        // default port is 8080 if not specified
        int port = 8080;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        
        LoadBalancer lb = new LoadBalancer(port);
        lb.start();
    }
    
    /**
     * starts the load balancer server
     */
    public void start() {
        System.out.println("load balancer starting on port " + loadBalancerPort);
        // TODO: implement server socket and client handling
    }
    
    /**
     * adds a backend server to the pool
     * @param host the backend server hostname
     * @param port the backend server port
     */
    public void addBackendServer(String host, int port) {
        backendServers.add(new BackendServer(host, port));
        System.out.println("added backend server: " + host + ":" + port);
    }
    
    /**
     * sets the load balancing algorithm
     * @param algorithm the algorithm to use
     */
    public void setAlgorithm(LoadBalancingAlgorithm algorithm) {
        this.algorithm = algorithm;
    }
}

