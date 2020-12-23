// Test case for issue 1264: https://github.com/typetools/checker-framework/issues/1264

import org.checkerframework.common.value.qual.*;

public class ValueCast2 {
    byte foo(@IntRange(from = Integer.MIN_VALUE, to = Integer.MAX_VALUE) int x) {
        return (byte) x;
    }

    byte bar(@IntRange(from = -1000, to = 500) int x) {
        return (byte) x;
    }

    short foo1(@IntRange(from = Integer.MIN_VALUE, to = Integer.MAX_VALUE) int x) {
        return (short) x;
    }

    int foo2(@IntRange(from = Long.MIN_VALUE, to = Long.MAX_VALUE) long x) {
        return (int) x;
    }

    int baz(@IntRange(from = Long.MIN_VALUE, to = 0) long x) {
        return (int) x;
    }
}
