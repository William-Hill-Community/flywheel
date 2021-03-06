package au.com.williamhill.flywheel.remote;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import com.obsidiandynamics.socketx.*;

import au.com.williamhill.flywheel.*;
import au.com.williamhill.flywheel.frame.*;

public final class RemoteNexus implements AutoCloseable {
  private final RemoteNode node;

  private final Map<UUID, CompletableFuture<BindResponseFrame>> bindRequests = new ConcurrentHashMap<>();
  
  private volatile String sessionId;
  
  private volatile XEndpoint endpoint;
  
  RemoteNexus(RemoteNode node) {
    this.node = node;
  }

  public XEndpoint getEndpoint() {
    return endpoint;
  }

  void setEndpoint(XEndpoint endpoint) {
    this.endpoint = endpoint;
  }
  
  public String getSessionId() {
    return sessionId;
  }
  
  void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public InetSocketAddress getPeerAddress() {
    return endpoint.getRemoteAddress();
  }
  
  CompletableFuture<BindResponseFrame> removeBindRequest(UUID id) {
    return bindRequests.remove(id);
  }
  
  public CompletableFuture<BindResponseFrame> bind(BindFrame bind) {
    final CompletableFuture<BindResponseFrame> future = new CompletableFuture<>();
    bindRequests.put(bind.getMessageId(), future);
    if (bind.getSessionId() != null) {
      setSessionId(bind.getSessionId());
    }
    SendHelper.send(bind, endpoint, node.getWire());
    return future;
  }
  
  public CompletableFuture<SendOutcome> publish(PublishTextFrame pub) {
    return SendHelper.send(pub, endpoint, node.getWire());
  }
  
  public void publish(PublishTextFrame pub, SendCallback callback) {
    SendHelper.send(pub, endpoint, node.getWire(), callback);
  }
  
  public CompletableFuture<SendOutcome> publish(PublishBinaryFrame pub) {
    return SendHelper.send(pub, endpoint, node.getWire());
  }
  
  public void publish(PublishBinaryFrame pub, SendCallback callback) {
    SendHelper.send(pub, endpoint, node.getWire(), callback);
  }
  
  @Override
  public void close() throws Exception {
    endpoint.close();
  }
  
  public boolean awaitClose(int waitMillis) throws InterruptedException {
    return endpoint.awaitClose(waitMillis);
  }

  @Override
  public String toString() {
    return "RemoteNexus [peer=" + getPeerAddress() + "]";
  }
}
