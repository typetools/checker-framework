import org.checkerframework.common.value.qual.IntRange;

public class BitwiseAnd {

  public static void Case11(@IntRange(from = 0) byte b) {
    @IntRange(from = 0, to = 0xf0) int i1 = b & 0xf0;
  }

  public static void Case12(@IntRange(to = -1) byte b) {
    @IntRange(from = 0, to = 0xf0) int i1 = b & 0xf0;
  }

  public static void Case13(byte b) {
    @IntRange(from = 0, to = 0xf0) int i1 = b & 0xf0;
  }

  public static void Case21(@IntRange(from = 0) byte b) {
    @IntRange(from = 0, to = 0x0f) long i1 = b & 0x800000000000000fL;
  }

  public static void Case22(@IntRange(to = -1) byte b) {
    @IntRange(from = 0x8000000000000000L, to = 0x80000000ffffffffL) long i1 = b & 0x80000000ffffffffL;
  }

  public static void Case23(byte b) {
    @IntRange(from = 0x8000000000000000L, to = 0xf0) long i1 = b & 0x80000000000000f0L;
  }

  public static void Issue1623(byte[] bytes) {
    for (int i = 0; i < bytes.length; i++) {
      byte b = bytes[i];
      @IntRange(from = 0, to = 15) int i1 = (b & 0xf0) >> 4;
      @IntRange(from = 0, to = 15) int i2 = b & 0x0f;
    }
  }
}
