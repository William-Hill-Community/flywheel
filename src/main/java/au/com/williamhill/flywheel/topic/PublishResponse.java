package au.com.williamhill.flywheel.topic;

public final class PublishResponse {
  private static final PublishResponse INSTANCE = new PublishResponse();
  
  static PublishResponse instance() { return INSTANCE; }
  
  private PublishResponse() {}

  @Override
  public String toString() {
    return "PublishResponse";
  }
}
