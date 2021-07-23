// Tests that parameters (including receiver parameters) marked as @Owning are still checked.

import org.checkerframework.checker.mustcall.qual.*;

class OwningParams {
    static void o1(@Owning OwningParams o) {}

    void o2(@Owning OwningParams this) {}

    void test(@Owning @MustCall({"a"}) OwningParams o, @Owning OwningParams p) {
        // :: error: argument.type.incompatible
        o1(o);
        // TODO: this error doesn't show up! See MustCallVisitor#skipReceiverSubtypeCheck
        //  error: method.invocation
        o.o2();
        o1(p);
        p.o2();
    }
}
