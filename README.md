# Java Socket Load Balancer

**Final Project for COSC4436 - Computer Networks**

A load balancer implementation in Java that distributes incoming client requests to multiple backend servers using socket-based communication. This project demonstrates key networking concepts including TCP socket programming, multi-threaded server design, and load balancing algorithms.

## Features

- **Load Balancing Algorithms**: Round-robin and least connections
- **Health Checking**: Automatic periodic health checks for backend servers
- **Retry Logic**: Automatic retry with different servers on connection failure
- **Statistics & Monitoring**: Real-time metrics tracking and periodic reporting
- **Multi-threaded**: Handles multiple concurrent client connections
- **Configurable**: Command-line configuration for port, algorithm, and backend servers

## Setup and Run Guide

### Prerequisites

- Java Development Kit (JDK) 8 or higher
- Command line/terminal access

### Compilation

Compile all Java source files:

javac *.java


This will create `.class` files for all components.

### Running the Load Balancer
#### Basic Usage

Run with default settings (port 8080, round-robin algorithm, default backend servers):


java LoadBalancer


#### Custom Port

Specify a custom port:


java LoadBalancer 8080


#### Custom Backend Servers

Specify backend servers in `host:port` format:


java LoadBalancer 8080 localhost:9001 localhost:9002 localhost:9003


#### Choose Load Balancing Algorithm
Use the `--algorithm` flag to select an algorithm:

# Round-robin (default)
java LoadBalancer 8080 --algorithm round-robin localhost:9001 localhost:9002

# Least connections
java LoadBalancer 8080 --algorithm least-connections localhost:9001 localhost:9002

#### Complete Example


java LoadBalancer 8080 --algorithm least-connections localhost:9001 localhost:9002 localhost:9003


### Testing the Load Balancer

To test the load balancer, you'll need to run backend servers on the ports you configure. The load balancer will:
- Accept client connections on its configured port
- Forward requests to backend servers
- Display statistics every 30 seconds
- Perform health checks every 10 seconds

## Code Documentation

### Project Structure

```
load_balancer/
├── LoadBalancer.java              # Main load balancer class
├── BackendServer.java              # Backend server representation
├── LoadBalancingAlgorithm.java     # Interface for load balancing algorithms
├── RoundRobinAlgorithm.java        # Round-robin implementation
├── LeastConnectionsAlgorithm.java # Least connections implementation
├── HealthChecker.java              # Health checking for backend servers
├── LoadBalancerStats.java          # Statistics tracking
└── README.md                       # This file
```

### Core Components

#### LoadBalancer.java
The main class that:
- Listens for client connections on a specified port
- Distributes requests to backend servers using the selected algorithm
- Manages health checking and statistics tracking
- Handles retry logic when backend connections fail
- Runs multiple threads to handle concurrent clients

#### BackendServer.java
Represents a backend server with:
- Host and port information
- Active connection tracking (for least connections algorithm)
- Health status (healthy/unhealthy)
- Last health check timestamp

#### Load Balancing Algorithms

**RoundRobinAlgorithm**: Cycles through servers in order, distributing requests evenly.

**LeastConnectionsAlgorithm**: Selects the server with the fewest active connections, useful for balancing load based on current server utilization.

#### HealthChecker.java
Background thread that:
- Periodically checks backend server health (every 10 seconds)
- Attempts TCP connections to verify server availability
- Updates server health status automatically
- Marks servers as unhealthy if they fail to respond

#### LoadBalancerStats.java
Tracks and reports:
- Total requests processed
- Successful and failed request counts
- Total connections established
- Success rate percentage
- Backend server status and active connections

### Key Networking Concepts Demonstrated

1. **TCP Socket Programming**: Uses Java's `ServerSocket` and `Socket` classes for network communication
2. **Multi-threading**: Each client connection handled in separate thread for concurrent processing
3. **Connection Forwarding**: Bidirectional data forwarding between client and backend servers
4. **Health Monitoring**: Periodic connectivity checks to ensure server availability
5. **Load Distribution**: Multiple algorithms for intelligent request routing
6. **Error Handling**: Retry logic and graceful failure handling

### Algorithm Selection

- **Round-robin**: Best for servers with similar capacity and when you want even distribution
- **Least connections**: Best when servers have different capacities or when connection duration varies significantly

### Statistics Output

Every 30 seconds, the load balancer prints statistics including:
- Total requests, successful requests, failed requests
- Success rate percentage
- Status of each backend server (healthy/unhealthy)
- Active connections per server

## Notes

- The load balancer assumes backend servers are already running on the specified ports
- Health checks run every 10 seconds with a 2-second connection timeout
- Statistics are printed every 30 seconds
- Connection retries attempt up to 3 different servers before failing
- All threads are daemon threads and will terminate when the main process exits

## Author
Jessica Garcia
Final project for COSC4436 - Computer Networks
