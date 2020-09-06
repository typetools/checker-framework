// Test case for https://tinyurl.com/cfissue/3614

// @skip-test until the issue is fixed

import org.checkerframework.checker.nullness.qual.PolyNull;

public class Issue3614 {

    public static @PolyNull Boolean not1(@PolyNull Boolean b) {
        return (b == null) ? null : !b;
    }

    public static @PolyNull Boolean not2(@PolyNull Boolean b) {
        if (b == null) {
            return null;
        } else {
            return !b;
        }
    }
}
