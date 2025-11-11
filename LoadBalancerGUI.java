import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * simple swing gui for the load balancer
 * displays statistics, server status, and provides controls
 */
public class LoadBalancerGUI extends JFrame {
    private LoadBalancer loadBalancer;
    private Thread loadBalancerThread;
    
    // configuration components
    private JTextField portField;
    private JComboBox<String> algorithmCombo;
    private JTextArea backendServersArea;
    private JButton startButton;
    private JButton stopButton;
    
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
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
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
        startButton = new JButton("Start Load Balancer");
        stopButton = new JButton("Stop Load Balancer");
        stopButton.setEnabled(false);
        
        startButton.addActionListener(e -> startLoadBalancer());
        stopButton.addActionListener(e -> stopLoadBalancer());
        
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        controlPanel.add(buttonPanel, BorderLayout.EAST);
        
        add(controlPanel, BorderLayout.SOUTH);
    }
    
    /**
     * starts the load balancer with current configuration
     */
    private void startLoadBalancer() {
        try {
            int port = Integer.parseInt(portField.getText());
            String algorithm = (String) algorithmCombo.getSelectedItem();
            String[] backendLines = backendServersArea.getText().split("\n");
            
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
                for (String line : backendLines) {
                    final String trimmedLine = line.trim();
                    if (!trimmedLine.isEmpty()) {
                        String[] parts = trimmedLine.split(":");
                        if (parts.length == 2) {
                            try {
                                String host = parts[0];
                                int backendPort = Integer.parseInt(parts[1]);
                                loadBalancer.addBackendServer(host, backendPort);
                            } catch (NumberFormatException e) {
                                final String errorLine = trimmedLine;
                                SwingUtilities.invokeLater(() -> {
                                    JOptionPane.showMessageDialog(this, 
                                        "Invalid backend server format: " + errorLine + "\nExpected: host:port",
                                        "Configuration Error", JOptionPane.ERROR_MESSAGE);
                                });
                            }
                        }
                    }
                }
                
                // start the load balancer
                loadBalancer.start();
            });
            
            loadBalancerThread.setDaemon(true);
            loadBalancerThread.start();
            
            // wait a moment for initialization
            Thread.sleep(500);
            
            updateUIState(true);
            statusLabel.setText("Status: Running on port " + port);
            statusLabel.setForeground(Color.GREEN);
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, 
                "Invalid port number. Please enter a valid integer.",
                "Configuration Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error starting load balancer: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
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

