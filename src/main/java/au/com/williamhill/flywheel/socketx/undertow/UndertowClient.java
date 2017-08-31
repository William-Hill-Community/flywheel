package au.com.williamhill.flywheel.socketx.undertow;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.net.ssl.*;

import org.xnio.*;
import org.xnio.ssl.*;

import au.com.williamhill.flywheel.socketx.*;
import io.undertow.connector.*;
import io.undertow.protocols.ssl.*;
import io.undertow.server.*;
import io.undertow.websockets.client.*;
import io.undertow.websockets.client.WebSocketClient.*;
import io.undertow.websockets.core.*;

public final class UndertowClient implements XClient<UndertowEndpoint> {
  private final XClientConfig config;
  
  private final XnioWorker worker;
  
  private final int bufferSize;
  
  private final XEndpointScanner<UndertowEndpoint> scanner;
  
  private UndertowClient(XClientConfig config, XnioWorker worker, int bufferSize) {
    this.config = config;
    this.worker = worker;
    this.bufferSize = bufferSize;
    scanner = new XEndpointScanner<>(config.scanIntervalMillis, 0);
  }

  @Override
  public UndertowEndpoint connect(URI uri, XEndpointListener<? super UndertowEndpoint> listener) throws Exception {
    final ByteBufferPool pool = new DefaultByteBufferPool(UndertowProperties.directBuffers, bufferSize);

    final ConnectionBuilder builder = WebSocketClient.connectionBuilder(worker, pool, uri);
    if (uri.getScheme().equals("wss")) {
      final SSLContext sslContext = config.sslContextProvider.getSSLContext();
      final ByteBufferPool sslBufferPool = new DefaultByteBufferPool(UndertowProperties.directBuffers, 17 * 1024);
      final XnioSsl ssl = new UndertowXnioSsl(worker.getXnio(), OptionMap.EMPTY, sslBufferPool, sslContext);
      builder.setSsl(ssl);
    }

    final WebSocketChannel channel = builder.connect().get(); 
    if (config.hasIdleTimeout()) {
      channel.setIdleTimeout(config.idleTimeoutMillis);
    }
    final UndertowEndpoint endpoint = UndertowEndpoint.clientOf(scanner, channel, config, listener);
    channel.getReceiveSetter().set(endpoint);
    channel.resumeReceives();
    return endpoint;
  }

  @Override
  public void close() throws Exception {
    scanner.close();
    worker.shutdown();
    worker.awaitTermination();
  }
  
  @Override
  public Collection<UndertowEndpoint> getEndpoints() {
    return scanner.getEndpoints();
  }
  
  @Override
  public XClientConfig getConfig() {
    return config;
  }
  
  public static final class Factory implements XClientFactory<UndertowEndpoint> {
    @Override public XClient<UndertowEndpoint> create(XClientConfig config) throws Exception {
      return new UndertowClient(config, createDefaultXnioWorker(), UndertowProperties.bufferSize);
    }
  }
  
  public static XClientFactory<UndertowEndpoint> factory() {
    return new Factory();
  }
  
  public static XClientFactory<UndertowEndpoint> factory(XnioWorker worker, int bufferSize) {
    return config -> new UndertowClient(config, worker, bufferSize);
  }
  
  public static XnioWorker createDefaultXnioWorker() throws IllegalArgumentException, IOException {
    return Xnio.getInstance().createWorker(OptionMap.builder()
                                           .set(Options.WORKER_IO_THREADS, UndertowProperties.ioThreads)
                                           .set(Options.THREAD_DAEMON, true)
                                           .set(Options.CONNECTION_HIGH_WATER, 1_000_000)
                                           .set(Options.CONNECTION_LOW_WATER, 1_000_000)
                                           .set(Options.WORKER_TASK_CORE_THREADS, UndertowProperties.coreTaskThreads)
                                           .set(Options.WORKER_TASK_MAX_THREADS, UndertowProperties.maxTaskThreads)
                                           .set(Options.TCP_NODELAY, true)
                                           .getMap());
  }
}
