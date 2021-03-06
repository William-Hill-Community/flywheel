package au.com.williamhill.flywheel.frame;

import java.util.*;

public final class BindFrame extends IdFrame implements TextEncodedFrame {
  public static String JSON_TYPE_NAME = "Bind";
  
  private String sessionId;
  
  private AuthCredentials auth;
  
  private String[] subscribe;
  
  private String[] unsubscribe;
  
  private Object metadata;
  
  public BindFrame() {
    this(null, null, null, null, null, null);
  }

  public BindFrame(UUID messageId, String sessionId, AuthCredentials auth, 
                   String[] subscribe, String[] unsubscribe, Object metadata) {
    super(messageId);
    this.sessionId = sessionId;
    this.auth = auth;
    this.subscribe = subscribe;
    this.unsubscribe = unsubscribe;
    this.metadata = metadata;
  }

  @Override
  public FrameType getType() {
    return FrameType.BIND;
  }
  
  public BindFrame withMessageId(UUID messageId) {
    setMessageId(messageId);
    return this;
  }
  
  public String getSessionId() {
    return sessionId;
  }
  
  public BindFrame withSessionId(String sessionId) {
    this.sessionId = sessionId;
    return this;
  }
  
  public AuthCredentials getAuth() {
    return auth;
  }
  
  public BindFrame withAuth(AuthCredentials auth) {
    this.auth = auth;
    return this;
  }

  public String[] getSubscribe() {
    return subscribe != null ? subscribe : new String[0];
  }
  
  public BindFrame withSubscribe(String ... subscribe) {
    this.subscribe = subscribe;
    return this;
  }

  public String[] getUnsubscribe() {
    return unsubscribe != null ? unsubscribe : new String[0];
  }
  
  public BindFrame withUnsubscribe(String ... unsubscribe) {
    this.unsubscribe = unsubscribe;
    return this;
  }

  public Object getMetadata() {
    return metadata;
  }
  
  public BindFrame withMetadata(Object metadata) {
    this.metadata = metadata;
    return this;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((auth == null) ? 0 : auth.hashCode());
    result = prime * result + ((metadata == null) ? 0 : metadata.hashCode());
    result = prime * result + ((sessionId == null) ? 0 : sessionId.hashCode());
    result = prime * result + Arrays.hashCode(getSubscribe());
    result = prime * result + Arrays.hashCode(getUnsubscribe());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    BindFrame other = (BindFrame) obj;
    if (auth == null) {
      if (other.auth != null)
        return false;
    } else if (!auth.equals(other.auth))
      return false;
    if (metadata == null) {
      if (other.metadata != null)
        return false;
    } else if (!metadata.equals(other.metadata))
      return false;
    if (sessionId == null) {
      if (other.sessionId != null)
        return false;
    } else if (!sessionId.equals(other.sessionId))
      return false;
    if (!Arrays.equals(getSubscribe(), other.getSubscribe()))
      return false;
    if (!Arrays.equals(getUnsubscribe(), other.getUnsubscribe()))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "BindFrame [messageId=" + getMessageId() + ", sessionId=" + sessionId + ", auth=" + auth + ", subscribe=" + Arrays.toString(subscribe)
           + ", unsubscribe=" + Arrays.toString(unsubscribe) + ", metadata=" + metadata + "]";
  }
}
