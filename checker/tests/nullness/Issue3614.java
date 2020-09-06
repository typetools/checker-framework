// Test case for https://tinyurl.com/cfissue/3614

// @skip-test until the issue is fixed

import org.checkerframework.checker.nullness.qual.PolyNull;

public class Issue3614 {

    public static @PolyNull Boolean not1(@PolyNull Boolean b) {
        return (b == null) ? null : !b;
    }

    public static @PolyNull Boolean not2(@PolyNull Boolean b) {
        return (b == null) ? b : !b;
    }

    public static @PolyNull Boolean not3(@PolyNull Boolean b) {
        if (b == null) {
            return null;
        } else {
            return !b;
        }
    }

    public static <T> T of1(T a) {
        return a == null ? null : a;
    }

    public static <T> T of2(T a) {
        if (a == null) {
            return null;
        } else {
            return a;
        }
    }

    public static @PolyNull Integer plus1(@PolyNull Integer b0, @PolyNull Integer b1) {
        return (b0 == null || b1 == null) ? null : (b0 + b1);
    }

    public static @PolyNull Integer plus2(@PolyNull Integer b0, @PolyNull Integer b1) {
        if (b0 == null || b1 == null) {
            return null;
        } else {
            return b0 + b1;
        }
    }
}
