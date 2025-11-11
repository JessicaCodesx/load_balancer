import java.io.*;
import java.net.*;

/**
 * simple test client for demonstrating the load balancer
 * connects to the load balancer and sends messages
 */
public class TestClient {
    private String host;
    private int port;
    
    /**
     * constructor for the test client
     * @param host the load balancer hostname
     * @param port the load balancer port
     */
    public TestClient(String host, int port) {
        this.host = host;
        this.port = port;
    }
    
    /**
     * connects to the load balancer and sends test messages
     */
    public void connect() {
        try (Socket socket = new Socket(host, port)) {
            System.out.println("connected to load balancer at " + host + ":" + port);
            
            // get input and output streams
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
            
            // read initial message from backend server (via load balancer)
            String response = in.readLine();
            if (response != null) {
                System.out.println("server response: " + response);
            }
            
            // interactive mode: send messages and receive responses
            System.out.println("type messages to send (or 'quit' to exit):");
            String userMessage;
            
            while ((userMessage = userInput.readLine()) != null) {
                // send message to server
                out.println(userMessage);
                
                // read response
                response = in.readLine();
                if (response != null) {
                    System.out.println("server response: " + response);
                }
                
                // exit on quit
                if (userMessage.equalsIgnoreCase("quit")) {
                    break;
                }
            }
            
            socket.close();
            System.out.println("disconnected from load balancer");
            
        } catch (IOException e) {
            System.err.println("error connecting to load balancer: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * sends a single test message and receives response
     * useful for automated testing
     */
    public void sendTestMessage(String message) {
        try (Socket socket = new Socket(host, port)) {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            
            // read initial welcome message
            String response = in.readLine();
            if (response != null) {
                System.out.println("server response: " + response);
            }
            
            // send test message
            out.println(message);
            
            // read response
            response = in.readLine();
            if (response != null) {
                System.out.println("server response: " + response);
            }
            
            // close connection
            out.println("quit");
            
        } catch (IOException e) {
            System.err.println("error in test: " + e.getMessage());
        }
    }
    
    /**
     * main method to run the test client
     * usage: java TestClient <loadBalancerHost> <loadBalancerPort> [message]
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("usage: java TestClient <loadBalancerHost> <loadBalancerPort> [message]");
            System.err.println("example: java TestClient localhost 8080");
            System.err.println("example: java TestClient localhost 8080 \"hello world\"");
            System.exit(1);
        }
        
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        
        TestClient client = new TestClient(host, port);
        
        if (args.length > 2) {
            // send single message
            client.sendTestMessage(args[2]);
        } else {
            // interactive mode
            client.connect();
        }
    }
}

