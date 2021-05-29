import org.checkerframework.common.value.qual.*;

public class EnumValues {

  public static enum Direction {
    NORTH,
    SOUTH,
    EAST,
    WEST
  };

  public static void enumValues() {
    Direction @ArrayLen(4) [] arr4 = Direction.values();
    Direction[] arr = Direction.values();
    Direction a = arr[0];
    Direction b = arr[1];
    Direction c = arr[2];
    Direction d = arr[3];
    // :: error: (array.access.unsafe.high.constant)
    Direction e = arr[4];
  }
}
