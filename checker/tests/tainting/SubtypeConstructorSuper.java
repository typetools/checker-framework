// Test case for issue 2264
// https://github.com/typetools/checker-framework/issues/2264

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class SubtypeConstructorSuper extends SuperClass {
    // :: error: (super.invocation.invalid)
    @Untainted SubtypeConstructorSuper() {
        super();
    }
}

class ImplicitSuperCall {
    // :: error: (super.invocation.invalid)
    @Untainted ImplicitSuperCall() {}
}

class SuperClass {
    @Tainted SuperClass() {}
}
