import testlib.util.*;

public class AnonymousClasses {

    void test() {
        new Object() {
            // TODO: the right hand side is
            // @Unqualified @Unqualified Object
            // We should make sure that the qualifier is only present once.

            // :: error: (assignment.type.incompatible)
            @Odd Object o = this; // error
        };

        // :: warning: (cast.unsafe.constructor.invocation)
        new @Odd Object() {
            @Odd Object o = this;
        };
    }
}
