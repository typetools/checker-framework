import testlib.util.*;

class ListOuter<T> {}

public class InnerGenerics {
    class ListInner<T> {}

    void testInner1() {
        // :: warning: (cast.unsafe.constructor.invocation)
        @Odd ListOuter<String> o = new @Odd ListOuter<String>();
        // :: warning: (cast.unsafe.constructor.invocation)
        @Odd ListInner<String> i = new @Odd ListInner<String>();
    }

    void testInner2() {
        // :: error: (assignment.type.incompatible)
        @Odd ListOuter<String> o = new ListOuter<>();
        // :: error: (assignment.type.incompatible)
        @Odd ListInner<String> i = new ListInner<>();
    }

    void testInner3() {
        ListOuter<@Odd String> o = new ListOuter<>();
        ListInner<@Odd String> i = new ListInner<>();
    }

    void testInner4() {
        // :: error: (assignment.type.incompatible)
        ListOuter<@Odd String> o = new ListOuter<String>();
        // :: error: (assignment.type.incompatible)
        ListInner<@Odd String> i = new ListInner<String>();
    }

    // more examples
}
