import org.checkerframework.checker.index.qual.LessThan;

public class LessThanFloat {
    int bigger;

    @LessThan("bigger") byte b;

    @LessThan("bigger") short s;

    @LessThan("bigger") int i;

    @LessThan("bigger") long l;

    // :: error: (anno.on.float)
    @LessThan("bigger") float f;

    // :: error: (anno.on.float)
    @LessThan("bigger") double d;

    // :: error: (anno.on.float)
    @LessThan("bigger") boolean bool;

    @LessThan("bigger") char c;
}
