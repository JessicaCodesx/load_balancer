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
        
        // set a nice background color
        getContentPane().setBackground(new Color(245, 245, 250));
        
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
        
        // create main panels with better layout
        createMainLayout();
        
        // set initial state
        updateUIState(false);
        
        // start update timer to refresh stats every second
        javax.swing.Timer updateTimer = new javax.swing.Timer(1000, e -> updateDisplay());
        updateTimer.start();
        
        pack();
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setResizable(true);
    }
    
    /**
     * creates the main layout with better organization
     */
    private void createMainLayout() {
        setLayout(new BorderLayout(10, 10));
        ((BorderLayout) getLayout()).setHgap(10);
        ((BorderLayout) getLayout()).setVgap(10);
        
        // top panel: configuration
        JPanel topPanel = createConfigPanel();
        add(topPanel, BorderLayout.NORTH);
        
        // center: split between stats and server status
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        centerSplit.setDividerLocation(500);
        centerSplit.setResizeWeight(0.5);
        centerSplit.setBorder(BorderFactory.createEmptyBorder());
        
        JPanel statsPanel = createStatsPanel();
        JPanel serverStatusPanel = createServerStatusPanel();
        
        centerSplit.setLeftComponent(statsPanel);
        centerSplit.setRightComponent(serverStatusPanel);
        add(centerSplit, BorderLayout.CENTER);
        
        // bottom: control panel
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.SOUTH);
    }
    
    /**
     * creates the configuration panel
     */
    private JPanel createConfigPanel() {
        JPanel configPanel = new JPanel();
        configPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 149, 237), 2),
                "Configuration",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font(Font.SANS_SERIF, Font.BOLD, 12),
                new Color(70, 70, 70)
            ),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        configPanel.setBackground(Color.WHITE);
        configPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        
        // port field
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel portLabel = new JLabel("Port:");
        portLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        configPanel.add(portLabel, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.3;
        portField = new JTextField("8080", 10);
        portField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        configPanel.add(portField, gbc);
        
        // algorithm selection
        gbc.gridx = 2; gbc.gridy = 0;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        JLabel algoLabel = new JLabel("Algorithm:");
        algoLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        configPanel.add(algoLabel, gbc);
        gbc.gridx = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.3;
        algorithmCombo = new JComboBox<>(new String[]{"round-robin", "least-connections"});
        algorithmCombo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        configPanel.add(algorithmCombo, gbc);
        
        // backend servers
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 4;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        JLabel serversLabel = new JLabel("Backend Servers (one per line, format: host:port):");
        serversLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        configPanel.add(serversLabel, gbc);
        gbc.gridy = 2;
        backendServersArea = new JTextArea("localhost:9001\nlocalhost:9002\nlocalhost:9003", 3, 30);
        backendServersArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        backendServersArea.setLineWrap(true);
        backendServersArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        JScrollPane scrollPane = new JScrollPane(backendServersArea);
        scrollPane.setPreferredSize(new Dimension(0, 80));
        configPanel.add(scrollPane, gbc);
        
        return configPanel;
    }
    
    /**
     * creates the statistics display panel
     */
    private JPanel createStatsPanel() {
        JPanel statsPanel = new JPanel();
        statsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 149, 237), 2),
                "Statistics",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font(Font.SANS_SERIF, Font.BOLD, 12),
                new Color(70, 70, 70)
            ),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        statsPanel.setBackground(Color.WHITE);
        statsPanel.setLayout(new GridLayout(5, 1, 10, 12));
        
        // create styled labels
        totalRequestsLabel = createStatLabel("Total Requests: 0");
        successfulRequestsLabel = createStatLabel("Successful: 0");
        failedRequestsLabel = createStatLabel("Failed: 0");
        successRateLabel = createStatLabel("Success Rate: 0.00%");
        totalConnectionsLabel = createStatLabel("Total Connections: 0");
        
        statsPanel.add(totalRequestsLabel);
        statsPanel.add(successfulRequestsLabel);
        statsPanel.add(failedRequestsLabel);
        statsPanel.add(successRateLabel);
        statsPanel.add(totalConnectionsLabel);
        
        return statsPanel;
    }
    
    /**
     * creates a styled statistics label
     */
    private JLabel createStatLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        label.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230, 230, 230)),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        label.setOpaque(true);
        label.setBackground(new Color(248, 248, 255));
        return label;
    }
    
    /**
     * creates the server status panel
     */
    private JPanel createServerStatusPanel() {
        JPanel statusPanel = new JPanel();
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 149, 237), 2),
                "Backend Server Status",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font(Font.SANS_SERIF, Font.BOLD, 12),
                new Color(70, 70, 70)
            ),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        statusPanel.setBackground(Color.WHITE);
        statusPanel.setLayout(new BorderLayout());
        
        serverStatusArea = new JTextArea(8, 30);
        serverStatusArea.setEditable(false);
        serverStatusArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        serverStatusArea.setBackground(new Color(248, 248, 255));
        serverStatusArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        JScrollPane scrollPane = new JScrollPane(serverStatusArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        statusPanel.add(scrollPane, BorderLayout.CENTER);
        
        return statusPanel;
    }
    
    /**
     * creates the control panel with start/stop buttons
     */
    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel();
        controlPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        controlPanel.setBackground(new Color(250, 250, 255));
        controlPanel.setLayout(new BorderLayout(15, 0));
        
        // status label with icon
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        statusPanel.setBackground(new Color(250, 250, 255));
        statusLabel = new JLabel("Status: Stopped");
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        statusLabel.setForeground(new Color(120, 120, 120));
        statusPanel.add(statusLabel);
        controlPanel.add(statusPanel, BorderLayout.WEST);
        
        // buttons panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(250, 250, 255));
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        
        // backend server buttons
        startBackendServersButton = createStyledButton("Start Backend Servers", 
            new Color(76, 175, 80), Color.WHITE);
        stopBackendServersButton = createStyledButton("Stop Backend Servers", 
            new Color(244, 67, 54), Color.WHITE);
        stopBackendServersButton.setEnabled(false);
        
        startBackendServersButton.addActionListener(e -> startBackendServers());
        stopBackendServersButton.addActionListener(e -> stopBackendServers());
        
        // load balancer buttons
        startButton = createStyledButton("Start Load Balancer", 
            new Color(33, 150, 243), Color.WHITE);
        stopButton = createStyledButton("Stop Load Balancer", 
            new Color(244, 67, 54), Color.WHITE);
        stopButton.setEnabled(false);
        
        startButton.addActionListener(e -> startLoadBalancer());
        stopButton.addActionListener(e -> stopLoadBalancer());
        
        buttonPanel.add(startBackendServersButton);
        buttonPanel.add(stopBackendServersButton);
        buttonPanel.add(new JSeparator(SwingConstants.VERTICAL) {
            {
                setPreferredSize(new Dimension(1, 30));
                setBackground(new Color(200, 200, 200));
            }
        });
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        controlPanel.add(buttonPanel, BorderLayout.EAST);
        
        return controlPanel;
    }
    
    /**
     * creates a styled button with colors
     */
    private JButton createStyledButton(String text, Color bgColor, Color textColor) {
        JButton button = new JButton(text);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        button.setBackground(bgColor);
        button.setForeground(textColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setPreferredSize(new Dimension(180, 35));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(bgColor.darker());
                }
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(bgColor);
                }
            }
        });
        
        return button;
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
                LoadBalancer newLB = new LoadBalancer(port);
                
                // set algorithm
                if ("least-connections".equals(algorithm)) {
                    newLB.setAlgorithm(new LeastConnectionsAlgorithm());
                } else {
                    newLB.setAlgorithm(new RoundRobinAlgorithm());
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
                                newLB.addBackendServer(host, backendPort);
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
                        statusLabel.setForeground(new Color(244, 67, 54));
                    });
                    return;
                }
                
                // show warning if some servers failed
                if (failedCount > 0) {
                    final int finalAddedCount = addedCount;
                    final int finalFailedCount = failedCount;
                    final String finalErrorMessages = errorMessages.toString();
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this,
                            "Added " + finalAddedCount + " backend server(s).\n\n" +
                            finalFailedCount + " server(s) failed to add:\n" + finalErrorMessages,
                            "Partial Success", JOptionPane.WARNING_MESSAGE);
                    });
                }
                
                // assign to volatile field before starting (so GUI can access it)
                loadBalancer = newLB;
                
                // start the load balancer
                newLB.start();
            });
            
            loadBalancerThread.setDaemon(true);
            loadBalancerThread.start();
            
            try {
                // wait a moment for initialization and check if load balancer was created
                Thread.sleep(500);
                
                // check if load balancer was created successfully
                LoadBalancer currentLB = loadBalancer;
                if (currentLB != null) {
                    updateUIState(true);
                    statusLabel.setText("Status: Running on port " + port);
                    statusLabel.setForeground(new Color(76, 175, 80));
                    
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
                    statusLabel.setForeground(new Color(244, 67, 54));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                JOptionPane.showMessageDialog(this,
                    "Interrupted while starting load balancer",
                    "Error", JOptionPane.ERROR_MESSAGE);
                updateUIState(false);
                statusLabel.setText("Status: Error");
                statusLabel.setForeground(new Color(244, 67, 54));
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    "Error starting load balancer: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
                updateUIState(false);
                statusLabel.setText("Status: Error");
                statusLabel.setForeground(new Color(244, 67, 54));
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
        statusLabel.setForeground(new Color(120, 120, 120));
        
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
        // get a local reference to avoid issues with volatile access
        LoadBalancer currentLB = loadBalancer;
        
        if (currentLB != null) {
            try {
                LoadBalancerStats stats = currentLB.getStats();
                List<BackendServer> servers = currentLB.getBackendServers();
                
                if (stats != null) {
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
                }
                
                // update server status
                if (servers != null && !servers.isEmpty()) {
                    StringBuilder statusText = new StringBuilder();
                    for (BackendServer server : servers) {
                        String healthStatus = server.isHealthy() ? "Healthy" : "Unhealthy";
                        statusText.append(String.format("%s:%d - %s (Connections: %d)\n",
                            server.getHost(), server.getPort(), healthStatus, server.getActiveConnections()));
                    }
                    serverStatusArea.setText(statusText.toString());
                } else {
                    serverStatusArea.setText("No backend servers configured");
                }
            } catch (Exception e) {
                // silently handle any errors during update
                // this prevents GUI from breaking if load balancer is in transition
            }
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

