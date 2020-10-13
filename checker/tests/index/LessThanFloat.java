import org.checkerframework.checker.index.qual.LessThan;

public class LessThanFloat {
    int bigger;

    @LessThan("bigger") byte b;

    @LessThan("bigger") short s;

    @LessThan("bigger") int i;

    @LessThan("bigger") long l;

    // :: error: (anno.on.nonintegral)
    @LessThan("bigger") float f;

    // :: error: (anno.on.nonintegral)
    @LessThan("bigger") double d;

    // :: error: (anno.on.nonintegral)
    @LessThan("bigger") boolean bool;

    @LessThan("bigger") char c;

    @LessThan("bigger") Byte bBoxed;

    @LessThan("bigger") Short sBoxed;

    @LessThan("bigger") Integer iBoxed;

    @LessThan("bigger") Long lBoxed;

    // :: error: (anno.on.nonintegral)
    @LessThan("bigger") Float fBoxed;

    // :: error: (anno.on.nonintegral)
    @LessThan("bigger") Double dBoxed;

    // :: error: (anno.on.nonintegral)
    @LessThan("bigger") Boolean boolBoxed;

    @LessThan("bigger") Character cBoxed;

    java.lang.@LessThan("bigger") Byte bBoxed2;

    java.lang.@LessThan("bigger") Short sBoxed2;

    java.lang.@LessThan("bigger") Integer iBoxed2;

    java.lang.@LessThan("bigger") Long lBoxed2;

    // :: error: (anno.on.nonintegral)
    java.lang.@LessThan("bigger") Float fBoxed2;

    // :: error: (anno.on.nonintegral)
    java.lang.@LessThan("bigger") Double dBoxed2;

    // :: error: (anno.on.nonintegral)
    java.lang.@LessThan("bigger") Boolean boolBoxed2;

    java.lang.@LessThan("bigger") Character cBoxed2;
}
