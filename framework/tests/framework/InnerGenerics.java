import testlib.util.*;

class ListOuter<T> {}

public class InnerGenerics {
    class ListInner<T> {}

    void testInner1() {
        @Odd ListOuter<String> o = new @Odd ListOuter<String>();
        @Odd ListInner<String> i = new @Odd ListInner<String>();
    }

    void testInner2() {
        // :: error: (assignment.type.incompatible)
        @Odd ListOuter<String> o = new ListOuter<String>();
        // :: error: (assignment.type.incompatible)
        @Odd ListInner<String> i = new ListInner<String>();
    }

    void testInner3() {
        ListOuter<@Odd String> o = new ListOuter<@Odd String>();
        ListInner<@Odd String> i = new ListInner<@Odd String>();
    }

    void testInner4() {
        // :: error: (assignment.type.incompatible)
        ListOuter<@Odd String> o = new ListOuter<String>();
        // :: error: (assignment.type.incompatible)
        ListInner<@Odd String> i = new ListInner<String>();
    }

    // more examples
}
