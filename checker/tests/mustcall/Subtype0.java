// A test that the @InheritedMustCall declaration annotation works correctly.

import org.checkerframework.checker.mustcall.qual.*;

@InheritableMustCall("a")
public class Subtype0 {

    public class Subtype1 extends Subtype0 {
        void m1() {}
    }

    public class Subtype2 extends Subtype1 {}

    static void test(
            @Owning Subtype0 s0,
            @Owning Subtype1 s1,
            @Owning Subtype2 s2,
            @Owning Subtype3 s3,
            @Owning Subtype4 s4) {
        // :: error: assignment
        @MustCall({}) Object obj1 = s0;
        @MustCall({"a"}) Object obj2 = s0;

        // :: error: assignment
        @MustCall({}) Object obj3 = s1;
        @MustCall({"a"}) Object obj4 = s1;

        // :: error: assignment
        @MustCall({}) Object obj5 = s2;
        @MustCall({"a"}) Object obj6 = s2;

        @MustCall({}) Object obj7 = s3;
        @MustCall({"a"}) Object obj8 = s3;

        @MustCall({}) Object obj9 = s4;
        @MustCall({"a"}) Object obj10 = s4;
    }

    @MustCall({})
    // :: error: inconsistent.mustcall.subtype :: error: super.invocation
    public class Subtype3 extends Subtype0 {}

    @InheritableMustCall({})
    // :: error: super.invocation
    public class Subtype4 extends Subtype0 {}

    @MustCall({"a"}) public class Subtype5 extends Subtype0 {}

    @InheritableMustCall({"a"})
    public class Subtype6 extends Subtype0 {}

    public class Container {
        Subtype0 in;

        void test() {
            if (in instanceof Subtype1) {
                ((Subtype1) in).m1();
            }
        }
    }
}
