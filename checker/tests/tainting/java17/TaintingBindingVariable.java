// @below-java16-jdk-skip-test
import org.checkerframework.checker.tainting.qual.Untainted;

public class TaintingBindingVariable {

    void bar(@Untainted Object o) {
        if (o instanceof @Untainted String s) {
            @Untainted String f = s;
        }
        if (o instanceof String s) {
            @Untainted String f2 = s;
        }
    }

    void bar2(Object o) {
        // :: warning: (instanceof.pattern.unsafe)
        if (o instanceof @Untainted String s) {
            @Untainted String f = s;
        }
        if (o instanceof String s) {
            // :: error: (assignment.type.incompatible)
            @Untainted String f2 = s;
        }
    }

    void bar3(Object o, boolean b) {
        // :: warning: (instanceof.pattern.unsafe)
        if (b && o instanceof @Untainted String s) {
            @Untainted String f = s;
        }
    }
}
