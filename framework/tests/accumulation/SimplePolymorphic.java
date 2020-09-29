// Tests that polymorphic annotations are supported by the Accumulation Checker.

import org.checkerframework.framework.testchecker.testaccumulation.qual.*;

class SimplePolymorphic {
    @PolyTestAccumulation
    Object id(@PolyTestAccumulation Object obj) {
        return obj;
    }

    @TestAccumulation("foo")
    Object usePoly(@TestAccumulation("foo") Object obj) {
        return id(obj);
    }
}
