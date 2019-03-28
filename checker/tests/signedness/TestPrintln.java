// Test case for issue #2366:
// https://github.com/typetools/checker-framework/issues/2366

import org.checkerframework.checker.signedness.qual.*;

public class TestPrintln {
    public static void main(String[] args) {
        // The first call produces the intended result, but the next two do not.
        // The Signedness Checker conservatively warns about all three, because it does not analyze
        // the specific values returned by parseUnsignedInt.

        @Unsigned int a = Integer.parseUnsignedInt("2147483647");
        // :: error: (argument.type.incompatible)
        System.out.println(a);
        @Unsigned int b = Integer.parseUnsignedInt("2147483648");
        // :: error: (argument.type.incompatible)
        System.out.println(b);
        @Unsigned int c = Integer.parseUnsignedInt("4000000000");
        // :: error: (argument.type.incompatible)
        System.out.println(c);
    }
}
