package au.com.williamhill.flywheel.edge.auth;

import java.io.*;
import java.net.*;
import java.security.*;

import javax.net.ssl.*;

import org.apache.http.*;
import org.apache.http.client.methods.*;
import org.apache.http.concurrent.*;
import org.apache.http.config.*;
import org.apache.http.entity.*;
import org.apache.http.impl.nio.client.*;
import org.apache.http.impl.nio.conn.*;
import org.apache.http.impl.nio.reactor.*;
import org.apache.http.nio.conn.*;
import org.apache.http.nio.conn.ssl.*;
import org.apache.http.nio.reactor.*;
import org.apache.http.ssl.*;
import org.apache.http.util.*;
import org.slf4j.*;

import com.google.gson.*;

import au.com.williamhill.flywheel.edge.*;

public final class ProxyHttpAuth implements Authenticator {
  private static final Logger LOG = LoggerFactory.getLogger(ProxyHttpAuth.class);

  private URI uri;

  private int poolSize = 8;
  
  private Gson gson;

  private CloseableHttpAsyncClient httpClient;

  public ProxyHttpAuth withUri(URI uri) {
    this.uri = uri;
    return this;
  }

  public ProxyHttpAuth withPoolSize(int poolSize) {
    this.poolSize = poolSize;
    return this;
  }

  @Override
  public void init() throws IOReactorException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
    gson = new GsonBuilder().disableHtmlEscaping().create();
    
    final HostnameVerifier hostnameVerifier = (s, sslSession) -> true;
    final Registry<SchemeIOSessionStrategy> sslSessionStrategy = RegistryBuilder
        .<SchemeIOSessionStrategy>create()
        .register("http", NoopIOSessionStrategy.INSTANCE)
        .register("https", new SSLIOSessionStrategy(getSSLContext(), hostnameVerifier)).build();

    final ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor();
    final PoolingNHttpClientConnectionManager cm = new PoolingNHttpClientConnectionManager(ioReactor, sslSessionStrategy);
    cm.setMaxTotal(poolSize);
    cm.setDefaultMaxPerRoute(poolSize);

    httpClient = HttpAsyncClients.custom().setConnectionManager(cm).build();
    httpClient.start();
  }

  private static SSLContext getSSLContext() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
    return SSLContexts.custom().loadTrustMaterial(null, (certificate, authType) -> true).build();
  }

  @Override
  public void verify(EdgeNexus nexus, String topic, AuthenticationOutcome outcome) {
    final ProxyAuthRequest authReq = new ProxyAuthRequest(nexus.getSession().getAuth(), topic);
    final String reqJson = gson.toJson(authReq);
    final StringEntity reqEntity = new StringEntity(reqJson, ContentType.APPLICATION_JSON);
    final HttpPost post = new HttpPost(uri);
    post.setEntity(reqEntity);
    post.setHeader("Accept", ContentType.APPLICATION_JSON.getMimeType());
    httpClient.execute(post, new FutureCallback<HttpResponse>() {
      @Override public void completed(HttpResponse res) {
        final int statusCode = res.getStatusLine().getStatusCode();
        switch (statusCode) {
          case 200:
          case 201:
            handleNormalResponse(topic, res, outcome);
            break;
            
          default:
            handleUnexpectedResponse(topic, reqJson, res, outcome);
            break;
        }
      }

      @Override public void failed(Exception cause) {
        handleSendFailure(topic, cause, outcome);
      }

      @Override public void cancelled() {}
    });
  }
  
  private void handleNormalResponse(String topic, HttpResponse res, AuthenticationOutcome outcome) {
    try {
      final String resJson = EntityUtils.toString(res.getEntity());
      final ProxyAuthResponse authRes = gson.fromJson(resJson, ProxyAuthResponse.class);
      if (authRes.getValidMillis() != null) {
        outcome.allow();
        if (LOG.isDebugEnabled()) LOG.debug("Allowing topic {} for {} ms", topic, authRes.getValidMillis());
      } else {
        outcome.forbidden(topic);
        if (LOG.isDebugEnabled()) LOG.debug("Denying topic {}", topic);
      }
    } catch (Throwable e) {
      outcome.forbidden(topic);
      LOG.warn("Error processing response", e);
    }
  }
  
  private static void handleUnexpectedResponse(String topic, String reqJson, HttpResponse res, AuthenticationOutcome outcome) {
    outcome.forbidden(topic);
    LOG.warn("Unexpected status code {} for request with entity-body {}", 
             res.getStatusLine().getStatusCode(), reqJson);
  }
  
  private static void handleSendFailure(String topic, Exception cause, AuthenticationOutcome outcome) {
    outcome.forbidden(topic);
    LOG.warn("Error sending request", cause);
  }
  
  @Override
  public void close() throws IOException {
    if (httpClient != null) {
      httpClient.close();
      httpClient = null;
    }
  }
}
