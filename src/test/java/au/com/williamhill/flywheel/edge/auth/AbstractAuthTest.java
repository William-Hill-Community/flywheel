package au.com.williamhill.flywheel.edge.auth;

import java.net.*;

import org.junit.*;

import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.socketx.*;
import com.obsidiandynamics.socketx.util.*;

import au.com.williamhill.flywheel.*;
import au.com.williamhill.flywheel.edge.*;
import au.com.williamhill.flywheel.frame.*;
import au.com.williamhill.flywheel.frame.Wire.*;
import au.com.williamhill.flywheel.remote.*;

public abstract class AbstractAuthTest {
  private static final int PREFERRED_PORT = 8080;
  private static final boolean SUPPRESS_LOGGING = true;
  
  private Wire wire;

  private RemoteNexusHandler handler;
 
  protected EdgeNode edge;
  
  protected RemoteNode remote;
  
  protected volatile Errors errors;
  
  protected volatile TextFrame text;
  
  protected volatile BinaryFrame binary;
  
  private int port;
  
  @Before
  public final void before() throws Exception {
    port = SocketUtils.getAvailablePort(PREFERRED_PORT);
    
    wire = new Wire(true, LocationHint.UNSPECIFIED);
    handler = new RemoteNexusHandlerBase() {
      @Override public void onText(RemoteNexus nexus, String topic, String payload) {
        if (topic.endsWith("/errors")) {
          errors = wire.decodeJson(payload, Errors.class);
        } else {
          text = new TextFrame(topic, payload);
        }
      }

      @Override public void onBinary(RemoteNexus nexus, String topic, byte[] payload) {
        binary = new BinaryFrame(topic, payload);
      }
    };
    
    remote = RemoteNode.builder()
        .withWire(wire)
        .build();
    
    setup();
  }
  
  protected void setup() throws Exception {}
  
  @After
  public final void after() throws Exception {
    teardown();
    if (remote != null) remote.close();
    if (edge != null) edge.close();
    remote = null;
    edge = null;
  }
  
  protected void teardown() throws Exception {}
  
  protected void clearReceived() {
    errors = null;
    text = null;
    binary = null;
  }
  
  protected void awaitReceived() {
    SocketUtils.await().untilTrue(() -> errors != null || text != null || binary != null);
  }
  
  protected void setupEdgeNode(AuthChain<PubAuthChain> pubAuthChain, AuthChain<SubAuthChain> subAuthChain) throws Exception {
    edge = EdgeNode.builder()
        .withServerConfig(new XServerConfig() {{ port = AbstractAuthTest.this.port; }})
        .withWire(wire)
        .withPubAuthChain(pubAuthChain)
        .withSubAuthChain(subAuthChain)
        .build();
    edge.setLoggingEnabled(! SUPPRESS_LOGGING || TestSupport.LOG);
    edge.addTopicListener(new TopicLambdaListener() {
      @Override public void onPublish(EdgeNexus nexus, PublishTextFrame pub) {
        if (pub.getTopic().endsWith("/tx")) {
          edge.publish(Flywheel.getRxTopicPrefix(nexus.getSession().getSessionId()), pub.getPayload());
        }
      }
      @Override public void onPublish(EdgeNexus nexus, PublishBinaryFrame pub) {
        if (pub.getTopic().endsWith("/tx")) {
          edge.publish(Flywheel.getRxTopicPrefix(nexus.getSession().getSessionId()), pub.getPayload());
        }
      }
    });
  }
  
  protected RemoteNexus openNexus() throws URISyntaxException, Exception {
    return remote.open(new URI("ws://localhost:" + port + "/"), 
                       InterceptingProxy.of(RemoteNexusHandler.class, handler, new LoggingInterceptor<>()));
  }
  
  protected String generateSessionId() {
    return Long.toHexString(Crypto.machineRandom());
  }
  
  protected static Authenticator createBasicAuth(String username, String password) {
    return new Authenticator() {
      @Override public void verify(EdgeNexus nexus, String topic, AuthenticationOutcome outcome) {
        if (nexus.getSession().getCredentials() instanceof BasicAuthCredentials) {
          final BasicAuthCredentials basic = nexus.getSession().getCredentials();
          if (username.equals(basic.getUsername()) && password.equals(basic.getPassword())) {
            outcome.allow(AuthenticationOutcome.INDEFINITE);
          } else {
            outcome.forbidden(topic);
          }
        } else {
          outcome.forbidden(topic);
        }
      }
    };
  }

  protected static Authenticator createBearerAuth(String token) {
    return new Authenticator() {
      @Override public void verify(EdgeNexus nexus, String topic, AuthenticationOutcome outcome) {
        if (nexus.getSession().getCredentials() instanceof BearerAuthCredentials) {
          final BearerAuthCredentials bearer = nexus.getSession().getCredentials();
          if (token.equals(bearer.getToken())) {
            outcome.allow(AuthenticationOutcome.INDEFINITE);
          } else {
            outcome.forbidden(topic);
          }
        } else {
          outcome.forbidden(topic);
        }
      }
    };
  }
}
