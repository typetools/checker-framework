import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class Issue3033 {

    void main() {
        @Tainted String a = getTainted();
        // :: warning: (instanceof.unsafe)
        if (a instanceof @Untainted String) {
            // `a` is now refined to @Untainted String
            isUntainted(a);
        }
    }

    static void isUntainted(@Untainted String a) {}

    static @Tainted String getTainted() {
        return "hi";
    }
}
