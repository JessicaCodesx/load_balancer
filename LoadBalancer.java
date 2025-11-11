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
        }
    }
    
    /**
     * handles a client connection by forwarding it to a backend server
     * @param clientSocket the socket connected to the client
     */
    private void handleClient(Socket clientSocket) {
        try {
            // select which backend server to use based on the algorithm
            BackendServer backend = algorithm.selectServer(backendServers);
            
            if (backend == null) {
                System.err.println("no backend servers available");
                clientSocket.close();
                return;
            }
            
            System.out.println("forwarding client to backend: " + backend);
            
            // connect to the selected backend server
            Socket backendSocket = new Socket(backend.getHost(), backend.getPort());
            
            // forward data between client and backend in both directions
            // this runs in separate threads so both directions work simultaneously
            Thread clientToBackend = new Thread(() -> {
                try {
                    forwardData(clientSocket.getInputStream(), backendSocket.getOutputStream());
                } catch (IOException e) {
                    // connection closed, that's ok
                }
            });
            
            Thread backendToClient = new Thread(() -> {
                try {
                    forwardData(backendSocket.getInputStream(), clientSocket.getOutputStream());
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
            
            System.out.println("client connection closed");
            
        } catch (Exception e) {
            System.err.println("error handling client: " + e.getMessage());
            e.printStackTrace();
            try {
                clientSocket.close();
            } catch (IOException ioException) {
                // ignore
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
     * sets the load balancing algorithm
     * @param algorithm the algorithm to use
     */
    public void setAlgorithm(LoadBalancingAlgorithm algorithm) {
        this.algorithm = algorithm;
    }
}

