package au.com.williamhill.flywheel.frame;

public enum FrameType {
  /** Bind request/response. */
  BIND (CharCodes.BIND, ByteCodes.BIND),
  
  /** Publish request. */
  PUBLISH (CharCodes.PUBLISH, (byte) ByteCodes.PUBLISH),
  
  /** Receive published message. */
  RECEIVE (CharCodes.RECEIVE, (byte) ByteCodes.RECEIVE); 
  
  private final char charCode;
  private final byte byteCode;
  
  private FrameType(char charCode, byte byteCode) {
    this.charCode = charCode;
    this.byteCode = byteCode;
  }

  final char getCharCode() {
    return charCode;
  }

  final byte getByteCode() {
    return byteCode;
  }
  
  private static final class CharCodes {
    public static final char BIND = 'B';
    public static final char PUBLISH = 'P';
    public static final char RECEIVE = 'R';
  }
  
  private static final class ByteCodes {
    public static final byte BIND = (byte) 0x10;
    public static final byte PUBLISH = (byte) 0x20;
    public static final byte RECEIVE = (byte) 0x30;
  }
  
  public static FrameType fromCharCode(char charCode) {
    switch (charCode) {
      case CharCodes.BIND: return FrameType.BIND;
      case CharCodes.PUBLISH: return FrameType.PUBLISH;
      case CharCodes.RECEIVE: return FrameType.RECEIVE;
      default: throw new IllegalArgumentException("Unsupported code " + charCode);
    }
  }
  
  public static FrameType fromByteCode(byte byteCode) {
    switch (byteCode) {
      case ByteCodes.BIND: return FrameType.BIND;
      case ByteCodes.PUBLISH: return FrameType.PUBLISH;
      case ByteCodes.RECEIVE: return FrameType.RECEIVE;
      default: throw new IllegalArgumentException("Unsupported code " + byteCode);
    }
  }
}
