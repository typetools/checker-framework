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
        Direction @MinLen(4) [] arr = Direction.values();
    }
}
