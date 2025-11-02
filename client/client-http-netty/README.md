# Smithy Java Client Netty HTTP Transport

This module provides a Netty-based HTTP transport implementation for Smithy Java clients, offering better resource utilization and async capabilities compared to the standard Java HttpClient.

## Features

- **Async I/O**: Built on Netty's non-blocking I/O for better performance
- **Resource Efficient**: Shared event loop groups for connection pooling
- **HTTP/1.1 and HTTP/2 Support**: Automatic protocol negotiation
- **SSL/TLS Support**: Built-in SSL support with proper certificate validation
- **Configurable Timeouts**: Request-level timeout configuration
- **Memory Efficient**: Direct ByteBuf handling without unnecessary copying
- **Streaming Request Bodies**: Supports both known-length and streaming request bodies
  - Known-length bodies are sent as `FullHttpRequest`
  - Unknown-length bodies use chunked transfer encoding with Flow.Publisher streaming

## Usage

### Basic Usage

```java
// Create the transport
NettyHttpClientTransport transport = new NettyHttpClientTransport();

// Use with a Smithy client
MyServiceClient client = MyServiceClient.builder()
    .transport(transport)
    .build();

// Don't forget to shutdown when done
transport.shutdown();
```

### With Custom EventLoopGroup

```java
// Create a shared event loop group
EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

// Create transport with shared event loop
NettyHttpClientTransport transport = new NettyHttpClientTransport(eventLoopGroup);

// Use the transport...

// Shutdown the event loop group when all transports are done
eventLoopGroup.shutdownGracefully();
```

### Configuration via smithy-build.json

```json
{
  "plugins": {
    "java-client-codegen": {
      "service": "com.example#MyService",
      "transport": {
        "name": "http-netty"
      }
    }
  }
}
```

### Streaming Request Bodies

The transport automatically handles both known-length and streaming request bodies:

```java
// Known-length body (sent as FullHttpRequest)
HttpRequest request1 = HttpRequest.builder()
    .uri(URI.create("https://example.com/api"))
    .method("POST")
    .body(DataStream.fromByteBuffer(ByteBuffer.wrap(data), "application/json"))
    .build();

// Streaming body (sent with chunked transfer encoding)
Flow.Publisher<ByteBuffer> streamingData = // your streaming data source
HttpRequest request2 = HttpRequest.builder()
    .uri(URI.create("https://example.com/api"))
    .method("POST")
    .body(DataStream.fromPublisher(streamingData, "application/octet-stream"))
    .build();
```

## Transport Factory

The module provides a `ClientTransportFactory` with the name `"http-netty"` that can be discovered automatically by the Smithy client framework.

## Dependencies

This module requires:
- Netty 4.1.100.Final or later
- Java 17 or later
- Smithy Java client-core and http-api modules

## Performance Considerations

- **Event Loop Sharing**: For multiple clients, consider sharing an EventLoopGroup to reduce thread overhead
- **Connection Pooling**: Netty automatically handles connection pooling and keep-alive
- **Memory Management**: The transport properly manages Netty ByteBuf lifecycle to prevent memory leaks

## Thread Safety

The NettyHttpClientTransport is thread-safe and can be used concurrently from multiple threads.
