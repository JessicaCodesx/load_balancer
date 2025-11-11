# Java Socket Load Balancer

A simple load balancer implementation in Java that distributes incoming client requests to multiple backend servers.

## Features

- Round-robin load balancing algorithm
- Support for multiple backend servers
- Socket-based communication

## Usage

Compile the Java files:
```
javac *.java
```

Run the load balancer:
```
java LoadBalancer [port]
```

Example:
```
java LoadBalancer 8080
```

## Project Structure

- `LoadBalancer.java` - Main load balancer class
- `BackendServer.java` - Represents a backend server
- `LoadBalancingAlgorithm.java` - Interface for load balancing algorithms
- `RoundRobinAlgorithm.java` - Round-robin implementation

