import org.checkerframework.checker.minlen.qual.*;

// @skip-test until the bug is fixed

class EnumValues {

    public static enum Direction {
        NORTH, SOUTH, EAST, WEST
    };

    public static void enumValues() {
        Direction @MinLen(4)[] arr = Direction.values();
    }
}
