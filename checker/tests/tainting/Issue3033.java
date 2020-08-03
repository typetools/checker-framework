import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

class Issue3033 {

    void main() {
        @Tainted String a = getTainted();
        // :: warning: (operand.instanceof.subtype)
        if (a
                instanceof
                @Untainted String) { // Since 'a' is @Tainted and reference type is @Untainted
            isUntainted(a); // 'a' is now refined to the reference type and hence, we get no error
        }
    }

    static void isUntainted(@Untainted String a) {}

    static @Tainted String getTainted() {
        return "hi";
    }
}
