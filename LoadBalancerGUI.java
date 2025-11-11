import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

/**
 * simple swing gui for the load balancer
 * displays statistics, server status, and provides controls
 */
public class LoadBalancerGUI extends JFrame {
    private volatile LoadBalancer loadBalancer;
    private Thread loadBalancerThread;
    
    // backend server management
    private Map<String, TestBackendServer> backendServers;
    private Map<String, Thread> backendServerThreads;
    
    // configuration components
    private JTextField portField;
    private JComboBox<String> algorithmCombo;
    private JTextArea backendServersArea;
    private JButton startButton;
    private JButton stopButton;
    private JButton startBackendServersButton;
    private JButton stopBackendServersButton;
    
    // statistics display components
    private JLabel totalRequestsLabel;
    private JLabel successfulRequestsLabel;
    private JLabel failedRequestsLabel;
    private JLabel successRateLabel;
    private JLabel totalConnectionsLabel;
    
    // server status components
    private JTextArea serverStatusArea;
    
    // status indicator
    private JLabel statusLabel;
    
    /**
     * constructor for the gui
     */
    public LoadBalancerGUI() {
        setTitle("Load Balancer - COSC4436");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // add window listener to clean up on close
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                // stop all backend servers before closing
                stopBackendServers();
                System.exit(0);
            }
        });
        
        // initialize backend server tracking
        backendServers = new HashMap<>();
        backendServerThreads = new HashMap<>();
        
        // create main panels
        createConfigPanel();
        createStatsPanel();
        createServerStatusPanel();
        createControlPanel();
        
        // set initial state
        updateUIState(false);
        
        // start update timer to refresh stats every second
        Timer updateTimer = new Timer(1000, e -> updateDisplay());
        updateTimer.start();
        
        pack();
        setSize(800, 600);
        setLocationRelativeTo(null);
    }
    
    /**
     * creates the configuration panel
     */
    private void createConfigPanel() {
        JPanel configPanel = new JPanel();
        configPanel.setBorder(new TitledBorder("Configuration"));
        configPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // port field
        gbc.gridx = 0; gbc.gridy = 0;
        configPanel.add(new JLabel("Port:"), gbc);
        gbc.gridx = 1;
        portField = new JTextField("8080", 10);
        configPanel.add(portField, gbc);
        
        // algorithm selection
        gbc.gridx = 0; gbc.gridy = 1;
        configPanel.add(new JLabel("Algorithm:"), gbc);
        gbc.gridx = 1;
        algorithmCombo = new JComboBox<>(new String[]{"round-robin", "least-connections"});
        configPanel.add(algorithmCombo, gbc);
        
        // backend servers
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        configPanel.add(new JLabel("Backend Servers:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        backendServersArea = new JTextArea("localhost:9001\nlocalhost:9002\nlocalhost:9003", 3, 20);
        backendServersArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(backendServersArea);
        configPanel.add(scrollPane, gbc);
        
        add(configPanel, BorderLayout.NORTH);
    }
    
    /**
     * creates the statistics display panel
     */
    private void createStatsPanel() {
        JPanel statsPanel = new JPanel();
        statsPanel.setBorder(new TitledBorder("Statistics"));
        statsPanel.setLayout(new GridLayout(3, 2, 10, 10));
        
        totalRequestsLabel = new JLabel("Total Requests: 0");
        successfulRequestsLabel = new JLabel("Successful: 0");
        failedRequestsLabel = new JLabel("Failed: 0");
        successRateLabel = new JLabel("Success Rate: 0.00%");
        totalConnectionsLabel = new JLabel("Total Connections: 0");
        
        statsPanel.add(totalRequestsLabel);
        statsPanel.add(successfulRequestsLabel);
        statsPanel.add(failedRequestsLabel);
        statsPanel.add(successRateLabel);
        statsPanel.add(totalConnectionsLabel);
        
        add(statsPanel, BorderLayout.CENTER);
    }
    
    /**
     * creates the server status panel
     */
    private void createServerStatusPanel() {
        JPanel statusPanel = new JPanel();
        statusPanel.setBorder(new TitledBorder("Backend Server Status"));
        statusPanel.setLayout(new BorderLayout());
        
        serverStatusArea = new JTextArea(8, 30);
        serverStatusArea.setEditable(false);
        serverStatusArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(serverStatusArea);
        statusPanel.add(scrollPane, BorderLayout.CENTER);
        
        add(statusPanel, BorderLayout.EAST);
    }
    
    /**
     * creates the control panel with start/stop buttons
     */
    private void createControlPanel() {
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BorderLayout());
        
        // status label
        statusLabel = new JLabel("Status: Stopped");
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        controlPanel.add(statusLabel, BorderLayout.WEST);
        
        // buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        
        // load balancer buttons
        startButton = new JButton("Start Load Balancer");
        stopButton = new JButton("Stop Load Balancer");
        stopButton.setEnabled(false);
        
        startButton.addActionListener(e -> startLoadBalancer());
        stopButton.addActionListener(e -> stopLoadBalancer());
        
        // backend server buttons
        startBackendServersButton = new JButton("Start Backend Servers");
        stopBackendServersButton = new JButton("Stop Backend Servers");
        stopBackendServersButton.setEnabled(false);
        
        startBackendServersButton.addActionListener(e -> startBackendServers());
        stopBackendServersButton.addActionListener(e -> stopBackendServers());
        
        buttonPanel.add(startBackendServersButton);
        buttonPanel.add(stopBackendServersButton);
        buttonPanel.add(new JSeparator(SwingConstants.VERTICAL));
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        controlPanel.add(buttonPanel, BorderLayout.EAST);
        
        add(controlPanel, BorderLayout.SOUTH);
    }
    
    /**
     * starts the load balancer with current configuration
     */
    private void startLoadBalancer() {
        // validate port number
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
            if (port < 1 || port > 65535) {
                JOptionPane.showMessageDialog(this,
                    "Port must be between 1 and 65535",
                    "Invalid Port", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                "Invalid port number. Please enter a valid integer between 1 and 65535.",
                "Invalid Port", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // validate backend servers
        String[] backendLines = backendServersArea.getText().split("\n");
        int validServerCount = 0;
        for (String line : backendLines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                String[] parts = trimmed.split(":");
                if (parts.length == 2) {
                    try {
                        Integer.parseInt(parts[1]);
                        validServerCount++;
                    } catch (NumberFormatException e) {
                        // invalid port, will be caught later
                    }
                }
            }
        }
        
        if (validServerCount == 0) {
            JOptionPane.showMessageDialog(this,
                "Please specify at least one backend server in the format: host:port\n" +
                "Example: localhost:9001",
                "No Backend Servers", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String algorithm = (String) algorithmCombo.getSelectedItem();
        
        // create load balancer in a separate thread
        loadBalancerThread = new Thread(() -> {
                // create load balancer
                loadBalancer = new LoadBalancer(port);
                
                // set algorithm
                if ("least-connections".equals(algorithm)) {
                    loadBalancer.setAlgorithm(new LeastConnectionsAlgorithm());
                } else {
                    loadBalancer.setAlgorithm(new RoundRobinAlgorithm());
                }
                
                // add backend servers
                int addedCount = 0;
                int failedCount = 0;
                StringBuilder errorMessages = new StringBuilder();
                
                for (String line : backendLines) {
                    final String trimmedLine = line.trim();
                    if (!trimmedLine.isEmpty()) {
                        String[] parts = trimmedLine.split(":");
                        if (parts.length == 2) {
                            try {
                                String host = parts[0];
                                int backendPort = Integer.parseInt(parts[1]);
                                loadBalancer.addBackendServer(host, backendPort);
                                addedCount++;
                            } catch (NumberFormatException e) {
                                failedCount++;
                                if (errorMessages.length() > 0) {
                                    errorMessages.append("\n");
                                }
                                errorMessages.append("Invalid format: ").append(trimmedLine);
                            }
                        } else {
                            failedCount++;
                            if (errorMessages.length() > 0) {
                                errorMessages.append("\n");
                            }
                            errorMessages.append("Invalid format: ").append(trimmedLine).append(" (expected host:port)");
                        }
                    }
                }
                
                // check if we have any valid servers
                if (addedCount == 0) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this,
                            "No valid backend servers were added.\n\n" + errorMessages.toString(),
                            "Configuration Error", JOptionPane.ERROR_MESSAGE);
                        updateUIState(false);
                        statusLabel.setText("Status: Failed to start");
                        statusLabel.setForeground(Color.RED);
                    });
                    return;
                }
                
                // show warning if some servers failed
                if (failedCount > 0) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this,
                            "Added " + addedCount + " backend server(s).\n\n" +
                            failedCount + " server(s) failed to add:\n" + errorMessages.toString(),
                            "Partial Success", JOptionPane.WARNING_MESSAGE);
                    });
                }
                
                // start the load balancer
                loadBalancer.start();
            });
            
            loadBalancerThread.setDaemon(true);
            loadBalancerThread.start();
            
            // wait a moment for initialization and check if load balancer was created
            Thread.sleep(500);
            
            // check if load balancer was created successfully
            LoadBalancer currentLB = loadBalancer;
            if (currentLB != null) {
                updateUIState(true);
                statusLabel.setText("Status: Running on port " + port);
                statusLabel.setForeground(Color.GREEN);
                
                // show success message
                JOptionPane.showMessageDialog(this,
                    "Load balancer started successfully!\n\n" +
                    "Port: " + port + "\n" +
                    "Algorithm: " + algorithm + "\n" +
                    "Backend servers configured",
                    "Load Balancer Started", JOptionPane.INFORMATION_MESSAGE);
            } else {
                updateUIState(false);
                statusLabel.setText("Status: Failed to start");
                statusLabel.setForeground(Color.RED);
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            JOptionPane.showMessageDialog(this,
                "Interrupted while starting load balancer",
                "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error starting load balancer: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            updateUIState(false);
            statusLabel.setText("Status: Error");
            statusLabel.setForeground(Color.RED);
        }
    }
    
    /**
     * stops the load balancer
     */
    private void stopLoadBalancer() {
        // note: the load balancer runs in an infinite loop, so we can't easily stop it
        // in a real implementation, we'd add a shutdown mechanism
        // for now, we'll just disable the UI
        updateUIState(false);
        statusLabel.setText("Status: Stopped");
        statusLabel.setForeground(Color.RED);
        
        JOptionPane.showMessageDialog(this,
            "Note: Load balancer thread will continue running.\n" +
            "Close the application to fully stop it.",
            "Info", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * starts all backend servers based on configuration
     */
    private void startBackendServers() {
        String[] backendLines = backendServersArea.getText().split("\n");
        int startedCount = 0;
        int failedCount = 0;
        StringBuilder errors = new StringBuilder();
        
        for (String line : backendLines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                String[] parts = trimmed.split(":");
                if (parts.length == 2) {
                    try {
                        String host = parts[0];
                        int port = Integer.parseInt(parts[1]);
                        String serverKey = host + ":" + port;
                        
                        // check if already running
                        if (backendServers.containsKey(serverKey) && 
                            backendServers.get(serverKey).isRunning()) {
                            continue; // already running
                        }
                        
                        // create and start backend server
                        String serverName = "Server-" + port;
                        TestBackendServer server = new TestBackendServer(port, serverName);
                        Thread serverThread = new Thread(() -> server.start());
                        serverThread.setDaemon(true);
                        serverThread.start();
                        
                        // wait a moment to see if it starts successfully
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                        
                        if (server.isRunning()) {
                            backendServers.put(serverKey, server);
                            backendServerThreads.put(serverKey, serverThread);
                            startedCount++;
                        } else {
                            failedCount++;
                            if (errors.length() > 0) errors.append("\n");
                            errors.append("Failed to start: ").append(serverKey);
                        }
                    } catch (NumberFormatException e) {
                        failedCount++;
                        if (errors.length() > 0) errors.append("\n");
                        errors.append("Invalid port in: ").append(trimmed);
                    } catch (Exception e) {
                        failedCount++;
                        if (errors.length() > 0) errors.append("\n");
                        errors.append("Error starting ").append(trimmed).append(": ").append(e.getMessage());
                    }
                }
            }
        }
        
        // update button states
        if (startedCount > 0) {
            startBackendServersButton.setEnabled(false);
            stopBackendServersButton.setEnabled(true);
        }
        
        // show result
        if (failedCount > 0) {
            JOptionPane.showMessageDialog(this,
                "Started " + startedCount + " backend server(s).\n\n" +
                failedCount + " server(s) failed:\n" + errors.toString(),
                "Partial Success", JOptionPane.WARNING_MESSAGE);
        } else if (startedCount > 0) {
            JOptionPane.showMessageDialog(this,
                "Successfully started " + startedCount + " backend server(s)!",
                "Backend Servers Started", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                "No backend servers were started. Please check your configuration.",
                "No Servers Started", JOptionPane.WARNING_MESSAGE);
        }
    }
    
    /**
     * stops all backend servers
     */
    private void stopBackendServers() {
        int stoppedCount = 0;
        
        for (Map.Entry<String, TestBackendServer> entry : backendServers.entrySet()) {
            TestBackendServer server = entry.getValue();
            if (server.isRunning()) {
                server.stop();
                stoppedCount++;
            }
        }
        
        backendServers.clear();
        backendServerThreads.clear();
        
        // update button states
        startBackendServersButton.setEnabled(true);
        stopBackendServersButton.setEnabled(false);
        
        if (stoppedCount > 0) {
            JOptionPane.showMessageDialog(this,
                "Stopped " + stoppedCount + " backend server(s).",
                "Backend Servers Stopped", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    /**
     * updates the ui state based on whether load balancer is running
     */
    private void updateUIState(boolean running) {
        startButton.setEnabled(!running);
        stopButton.setEnabled(running);
        portField.setEnabled(!running);
        algorithmCombo.setEnabled(!running);
        backendServersArea.setEnabled(!running);
    }
    
    /**
     * updates the display with current statistics
     */
    private void updateDisplay() {
        if (loadBalancer != null && loadBalancer.getStats() != null) {
            LoadBalancerStats stats = loadBalancer.getStats();
            List<BackendServer> servers = loadBalancer.getBackendServers();
            
            // update statistics labels
            totalRequestsLabel.setText("Total Requests: " + stats.getTotalRequests());
            successfulRequestsLabel.setText("Successful: " + stats.getSuccessfulRequests());
            failedRequestsLabel.setText("Failed: " + stats.getFailedRequests());
            totalConnectionsLabel.setText("Total Connections: " + stats.getTotalConnections());
            
            // calculate and display success rate
            if (stats.getTotalRequests() > 0) {
                double successRate = (double) stats.getSuccessfulRequests() / stats.getTotalRequests() * 100;
                successRateLabel.setText(String.format("Success Rate: %.2f%%", successRate));
            } else {
                successRateLabel.setText("Success Rate: 0.00%");
            }
            
            // update server status
            StringBuilder statusText = new StringBuilder();
            if (servers != null && !servers.isEmpty()) {
                for (BackendServer server : servers) {
                    String healthStatus = server.isHealthy() ? "✓ Healthy" : "✗ Unhealthy";
                    Color statusColor = server.isHealthy() ? Color.GREEN : Color.RED;
                    statusText.append(String.format("%s:%d - %s (Connections: %d)\n",
                        server.getHost(), server.getPort(), healthStatus, server.getActiveConnections()));
                }
            } else {
                statusText.append("No backend servers configured");
            }
            serverStatusArea.setText(statusText.toString());
        } else {
            // reset display when not running
            totalRequestsLabel.setText("Total Requests: 0");
            successfulRequestsLabel.setText("Successful: 0");
            failedRequestsLabel.setText("Failed: 0");
            successRateLabel.setText("Success Rate: 0.00%");
            totalConnectionsLabel.setText("Total Connections: 0");
            serverStatusArea.setText("Load balancer not running");
        }
    }
    
    /**
     * main method to launch the gui
     */
    public static void main(String[] args) {
        // set look and feel to system default
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // use default look and feel
        }
        
        SwingUtilities.invokeLater(() -> {
            new LoadBalancerGUI().setVisible(true);
        });
    }
}

