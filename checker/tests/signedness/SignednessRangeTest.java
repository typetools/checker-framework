import org.checkerframework.checker.signedness.qual.Unsigned;

class SignednessRangeTest {

  private static final @Unsigned int UINT8_MAX = 255;
  private static final @Unsigned int UINT16_MAX = 65_535;
  private static final @Unsigned byte SOFT_MAX = (@Unsigned byte) UINT8_MAX;
  private static final @Unsigned byte SOFT_MAX_2 = (@Unsigned byte) 255;
  private static final @Unsigned short DISTANCE_MAX = (@Unsigned short) UINT16_MAX;
}
