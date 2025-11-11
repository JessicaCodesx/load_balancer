import java.io.*;
import java.net.*;

/**
 * simple test backend server for demonstrating the load balancer
 * this server listens on a port and responds to client connections
 */
public class TestBackendServer {
    private int port;
    private String serverName;
    private volatile boolean running;
    private ServerSocket serverSocket;
    
    /**
     * constructor for the test backend server
     * @param port the port to listen on
     * @param serverName a name to identify this server in responses
     */
    public TestBackendServer(int port, String serverName) {
        this.port = port;
        this.serverName = serverName;
        this.running = false;
    }
    
    /**
     * starts the backend server
     */
    public void start() {
        running = true;
        System.out.println(serverName + " starting on port " + port);
        
        try {
            serverSocket = new ServerSocket(port);
            System.out.println(serverName + " listening on port " + port);
            System.out.println("ready to accept connections from load balancer");
            
            // keep accepting connections while running
            while (running) {
                try {
                    // wait for a connection from the load balancer
                    Socket clientSocket = serverSocket.accept();
                    System.out.println(serverName + " received connection from: " + clientSocket.getRemoteSocketAddress());
                    
                    // handle each connection in a separate thread
                    new Thread(() -> handleClient(clientSocket)).start();
                } catch (IOException e) {
                    if (running) {
                        System.err.println("error accepting connection in " + serverName + ": " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("error in " + serverName + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            stop();
        }
    }
    
    /**
     * stops the backend server
     */
    public void stop() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                System.out.println(serverName + " stopped");
            } catch (IOException e) {
                System.err.println("error closing server socket: " + e.getMessage());
            }
        }
    }
    
    /**
     * checks if the server is running
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return running;
    }
    
    /**
     * handles a client connection
     * @param socket the socket connected to the client (via load balancer)
     */
    private void handleClient(Socket socket) {
        try {
            // get input and output streams
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            
            // send a welcome message identifying this server
            out.println("hello from " + serverName + " on port " + port);
            
            // read and echo back messages from the client
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                // echo back with server identification
                out.println(serverName + " received: " + inputLine);
                
                // if client sends "quit", close the connection
                if (inputLine.equalsIgnoreCase("quit")) {
                    break;
                }
            }
            
            socket.close();
            System.out.println(serverName + " closed connection");
            
        } catch (IOException e) {
            System.err.println("error handling client in " + serverName + ": " + e.getMessage());
        }
    }
    
    /**
     * main method to run a test backend server
     * usage: java TestBackendServer <port> [serverName]
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("usage: java TestBackendServer <port> [serverName]");
            System.err.println("example: java TestBackendServer 9001 Server1");
            System.exit(1);
        }
        
        int port = Integer.parseInt(args[0]);
        String serverName = args.length > 1 ? args[1] : "BackendServer-" + port;
        
        TestBackendServer server = new TestBackendServer(port, serverName);
        server.start();
    }
}

