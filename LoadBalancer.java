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
    private HealthChecker healthChecker;
    private Thread healthCheckerThread;
    private LoadBalancerStats stats;
    
    /**
     * constructor to initialize the load balancer
     * @param port the port the load balancer will listen on
     */
    public LoadBalancer(int port) {
        this.loadBalancerPort = port;
        this.backendServers = new ArrayList<>();
        // default to round-robin algorithm
        this.algorithm = new RoundRobinAlgorithm();
        this.stats = new LoadBalancerStats();
    }
    
    /**
     * main method to start the load balancer
     */
    public static void main(String[] args) {
        // default port is 8080 if not specified
        int port = 8080;
        String algorithmName = "round-robin"; // default algorithm
        List<String> backendServerArgs = new ArrayList<>();
        
        // parse command line arguments
        // format: java LoadBalancer [port] [--algorithm round-robin|least-connections] [backend1:port] [backend2:port] ...
        int argIndex = 0;
        
        // first arg might be port number
        if (args.length > 0 && !args[0].startsWith("--")) {
            try {
                port = Integer.parseInt(args[0]);
                argIndex = 1;
            } catch (NumberFormatException e) {
                // not a number, might be backend server
            }
        }
        
        // look for algorithm flag
        while (argIndex < args.length) {
            if (args[argIndex].equals("--algorithm") && argIndex + 1 < args.length) {
                algorithmName = args[argIndex + 1].toLowerCase();
                argIndex += 2;
            } else {
                // assume it's a backend server
                backendServerArgs.add(args[argIndex]);
                argIndex++;
            }
        }
        
        LoadBalancer lb = new LoadBalancer(port);
        
        // set the load balancing algorithm
        if (algorithmName.equals("least-connections") || algorithmName.equals("least")) {
            lb.setAlgorithm(new LeastConnectionsAlgorithm());
            System.out.println("using least connections algorithm");
        } else if (algorithmName.equals("round-robin") || algorithmName.equals("roundrobin")) {
            lb.setAlgorithm(new RoundRobinAlgorithm());
            System.out.println("using round-robin algorithm");
        } else {
            System.err.println("unknown algorithm: " + algorithmName + ", using round-robin");
            lb.setAlgorithm(new RoundRobinAlgorithm());
        }
        
        // add backend servers
        if (!backendServerArgs.isEmpty()) {
            // parse backend servers from command line arguments
            // example: java LoadBalancer 8080 localhost:9001 localhost:9002 localhost:9003
            for (String arg : backendServerArgs) {
                String[] parts = arg.split(":");
                if (parts.length == 2) {
                    String host = parts[0];
                    int backendPort = Integer.parseInt(parts[1]);
                    lb.addBackendServer(host, backendPort);
                } else {
                    System.err.println("invalid backend server format: " + arg + " (expected host:port)");
                }
            }
        } else {
            // use default backend servers for testing
            // these are just examples - in production you'd configure these properly
            System.out.println("no backend servers specified, using defaults");
            lb.addBackendServer("localhost", 9001);
            lb.addBackendServer("localhost", 9002);
            lb.addBackendServer("localhost", 9003);
        }
        
        // check if we have any backend servers
        if (!lb.hasBackendServers()) {
            System.err.println("error: no backend servers configured!");
            System.err.println("usage: java LoadBalancer [port] [--algorithm round-robin|least-connections] [backend1:port] [backend2:port] ...");
            System.exit(1);
        }
        
        lb.start();
    }
    
    /**
     * starts the load balancer server
     */
    public void start() {
        System.out.println("load balancer starting on port " + loadBalancerPort);
        
        // start health checker in background
        // checks every 10 seconds with 2 second connection timeout
        healthChecker = new HealthChecker(backendServers, 10, 2000);
        healthCheckerThread = new Thread(healthChecker);
        healthCheckerThread.setDaemon(true); // dies when main thread dies
        healthCheckerThread.start();
        
        // start stats printer thread - prints stats every 30 seconds
        Thread statsThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(30000); // 30 seconds
                    stats.printStats(backendServers);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        statsThread.setDaemon(true);
        statsThread.start();
        
        try (ServerSocket serverSocket = new ServerSocket(loadBalancerPort)) {
            System.out.println("load balancer listening on port " + loadBalancerPort);
            
            // keep accepting client connections
            while (true) {
                // wait for a client to connect
                Socket clientSocket = serverSocket.accept();
                System.out.println("new client connected: " + clientSocket.getRemoteSocketAddress());
                
                // handle each client in a separate thread so we can handle multiple clients
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("error starting load balancer: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // stop health checker when load balancer stops
            if (healthChecker != null) {
                healthChecker.stop();
            }
        }
    }
    
    /**
     * gets a list of only the healthy backend servers
     * @return list of healthy servers
     */
    private List<BackendServer> getHealthyServers() {
        List<BackendServer> healthyServers = new ArrayList<>();
        for (BackendServer server : backendServers) {
            if (server.isHealthy()) {
                healthyServers.add(server);
            }
        }
        return healthyServers;
    }
    
    /**
     * handles a client connection by forwarding it to a backend server
     * includes retry logic if connection to backend fails
     * @param clientSocket the socket connected to the client
     */
    private void handleClient(Socket clientSocket) {
        BackendServer backend = null;
        Socket backendSocket = null;
        int maxRetries = 3; // try up to 3 different servers
        boolean requestSuccessful = false;
        
        // track that we received a request
        stats.incrementTotalRequests();
        
        try {
            // try to connect to a backend server, with retries if it fails
            for (int attempt = 0; attempt < maxRetries; attempt++) {
                // get only healthy servers and select one based on the algorithm
                List<BackendServer> healthyServers = getHealthyServers();
                
                if (healthyServers.isEmpty()) {
                    System.err.println("no healthy backend servers available");
                    stats.incrementFailedRequests();
                    clientSocket.close();
                    return;
                }
                
                backend = algorithm.selectServer(healthyServers);
                
                if (backend == null) {
                    System.err.println("failed to select a backend server");
                    clientSocket.close();
                    return;
                }
                
                try {
                    // increment connection count for this backend server
                    // this is important for least connections algorithm
                    backend.incrementConnections();
                    
                    System.out.println("forwarding client to backend: " + backend + 
                                     " (connections: " + backend.getActiveConnections() + ")");
                    
                    // try to connect to the selected backend server
                    // set a timeout so we don't hang forever
                    backendSocket = new Socket();
                    backendSocket.connect(new java.net.InetSocketAddress(backend.getHost(), backend.getPort()), 5000);
                    
                    // connection successful, break out of retry loop
                    break;
                    
                } catch (IOException e) {
                    // connection failed, mark server as unhealthy and try another
                    System.err.println("failed to connect to " + backend + ": " + e.getMessage());
                    backend.setHealthy(false);
                    backend.decrementConnections(); // undo the increment since we failed
                    
                    if (attempt < maxRetries - 1) {
                        System.out.println("retrying with another backend server (attempt " + (attempt + 2) + "/" + maxRetries + ")");
                        backend = null;
                        backendSocket = null;
                    } else {
                        // out of retries
                        System.err.println("failed to connect to any backend server after " + maxRetries + " attempts");
                        stats.incrementFailedRequests();
                        clientSocket.close();
                        return;
                    }
                }
            }
            
            // if we got here, we have a successful connection
            if (backendSocket == null || backend == null) {
                System.err.println("failed to establish backend connection");
                stats.incrementFailedRequests();
                clientSocket.close();
                return;
            }
            
            // track successful connection
            stats.incrementTotalConnections();
            
            // create final references for use in lambda expressions
            final Socket finalBackendSocket = backendSocket;
            final Socket finalClientSocket = clientSocket;
            
            // forward data between client and backend in both directions
            // this runs in separate threads so both directions work simultaneously
            Thread clientToBackend = new Thread(() -> {
                try {
                    forwardData(finalClientSocket.getInputStream(), finalBackendSocket.getOutputStream());
                } catch (IOException e) {
                    // connection closed, that's ok
                }
            });
            
            Thread backendToClient = new Thread(() -> {
                try {
                    forwardData(finalBackendSocket.getInputStream(), finalClientSocket.getOutputStream());
                } catch (IOException e) {
                    // connection closed, that's ok
                }
            });
            
            clientToBackend.start();
            backendToClient.start();
            
            // wait for both forwarding threads to finish
            clientToBackend.join();
            backendToClient.join();
            
            // close connections when done
            clientSocket.close();
            backendSocket.close();
            
            // mark request as successful since we completed the connection
            requestSuccessful = true;
            stats.incrementSuccessfulRequests();
            
            System.out.println("client connection closed");
            
        } catch (Exception e) {
            System.err.println("error handling client: " + e.getMessage());
            e.printStackTrace();
            // if we didn't already mark it as failed, mark it now
            if (!requestSuccessful) {
                stats.incrementFailedRequests();
            }
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
                if (backendSocket != null) {
                    backendSocket.close();
                }
            } catch (IOException ioException) {
                // ignore
            }
        } finally {
            // always decrement connection count when done
            // this ensures proper tracking even if errors occur
            if (backend != null) {
                backend.decrementConnections();
            }
        }
    }
    
    /**
     * forwards data from input stream to output stream
     * @param input the stream to read from
     * @param output the stream to write to
     */
    private void forwardData(InputStream input, OutputStream output) {
        try {
            byte[] buffer = new byte[4096];
            int bytesRead;
            
            // read data and write it to the other stream
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
                output.flush();
            }
        } catch (IOException e) {
            // connection closed or error, stop forwarding
        }
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
     * checks if there are any backend servers configured
     * @return true if there are backend servers, false otherwise
     */
    public boolean hasBackendServers() {
        return !backendServers.isEmpty();
    }
    
    /**
     * gets the statistics object for monitoring
     * @return the LoadBalancerStats object
     */
    public LoadBalancerStats getStats() {
        return stats;
    }
    
    /**
     * gets the list of backend servers
     * @return list of backend servers
     */
    public List<BackendServer> getBackendServers() {
        return backendServers;
    }
    
    /**
     * sets the load balancing algorithm
     * @param algorithm the algorithm to use
     */
    public void setAlgorithm(LoadBalancingAlgorithm algorithm) {
        this.algorithm = algorithm;
    }
}

