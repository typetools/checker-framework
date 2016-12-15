import org.checkerframework.checker.minlen.qual.*;
import org.checkerframework.common.value.qual.*;

class EnumValues {

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
        // warning: (array.access.unsafe.high)
        Direction e = arr[4];
    }
}
