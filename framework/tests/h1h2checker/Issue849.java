// Test case for Issue 849:
// https://github.com/typetools/checker-framework/issues/849

import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S2;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1Top;

class Issue849 {
    class Gen<G> {}

    void polyAll(Gen<Gen<@H1S2 Object>> genGenNonNull) {
        // :: error: (assignment.type.incompatible)
        Gen<@H1Top ? extends @H1Top Gen<@H1Top Object>> a = genGenNonNull;
    }
}
