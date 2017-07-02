package au.com.williamhill.flywheel.socketx;

import static java.util.concurrent.TimeUnit.*;
import static org.awaitility.Awaitility.*;

import java.nio.*;
import java.util.*;

import org.junit.*;
import org.mockito.*;

import com.obsidiandynamics.indigo.util.*;

import au.com.williamhill.flywheel.socketx.jetty.*;
import au.com.williamhill.flywheel.socketx.netty.*;
import au.com.williamhill.flywheel.socketx.undertow.*;
import au.com.williamhill.flywheel.util.*;

public final class EchoTest extends BaseClientServerTest {
  private static final int CYCLES = 2;
  private static final int CONNECTIONS = 5;
  private static final int MESSAGES = 10;
  private static final int PROGRESS_INTERVAL = 10;
  private static final int MAX_PORT_USE_COUNT = 10_000;

  @Test
  public void testJtJt() throws Exception {
    test(CYCLES, CONNECTIONS, MESSAGES, JettyServer.factory(), JettyClient.factory());
  }

  @Test
  public void testUtUt() throws Exception {
    test(CYCLES, CONNECTIONS, MESSAGES, UndertowServer.factory(), UndertowClient.factory());
  }

  @Test
  public void testNtUt() throws Exception {
    test(CYCLES, CONNECTIONS, MESSAGES, NettyServer.factory(), UndertowClient.factory());
  }

  private void test(int cycles, int connections, int messages,
                    XServerFactory<? extends XEndpoint> serverFactory,
                    XClientFactory<? extends XEndpoint> clientFactory) throws Exception {
    for (int cycle = 0; cycle < cycles; cycle++) {
      if (cycle != 0) init();
      test(connections, messages, serverFactory, clientFactory);
      dispose();
      if (PROGRESS_INTERVAL != 0 && cycle % PROGRESS_INTERVAL == PROGRESS_INTERVAL - 1) {
        LOG_STREAM.format("cycle %,d\n", cycle);
      }
    }
  }

  private void test(int connections, int messages,
                    XServerFactory<? extends XEndpoint> serverFactory,
                    XClientFactory<? extends XEndpoint> clientFactory) throws Exception {
    final XServerConfig serverConfig = getDefaultServerConfig();
    serverConfig.scanIntervalMillis = 1;
    final XEndpointListener<XEndpoint> serverListener = new XEndpointLambdaListener<>()
        .onText((endpoint, message) -> endpoint.send(message, null))
        .onBinary((endpoint, message) -> endpoint.send(message, null));
    createServer(serverFactory, serverConfig, serverListener);

    final XClientConfig clientConfig = getDefaultClientConfig();
    clientConfig.scanIntervalMillis = 1;
    createClient(clientFactory, clientConfig);
    final XEndpointListener<XEndpoint> clientListener = createMockListener();
    final List<XEndpoint> endpoints = new ArrayList<>(connections);
    
    // connect all endpoints
    for (int i = 0; i < connections; i++) {
      endpoints.add(openClientEndpoint(serverConfig.port, clientListener));
    }
    
    // send one text and one binary frame over each client connection
    for (XEndpoint endpoint : endpoints) {
      for (int i = 0; i < messages; i++) {
        endpoint.send("test", null);
        endpoint.send(toBuffer("test"), null);
      }
    }
    
    // assert receival of echo on clients
    final int expected = connections * messages;
    await().dontCatchUncaughtExceptions().atMost(60, SECONDS).untilAsserted(() -> {
      Mockito.verify(clientListener, Mockito.times(expected)).onText(Mocks.anyNotNull(), 
                                                                     Mockito.eq("test"));
      Mockito.verify(clientListener, Mockito.times(expected)).onBinary(Mocks.anyNotNull(), 
                                                                       Mockito.eq(toBuffer("test")));
    });

    SocketTestSupport.drainPort(serverConfig.port, MAX_PORT_USE_COUNT);
  }
  
  private static ByteBuffer toBuffer(String str) {
    return ByteBuffer.wrap(str.getBytes());
  }
}