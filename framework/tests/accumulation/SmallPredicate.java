// small test case for predicates, for debugging

import org.checkerframework.framework.testchecker.testaccumulation.qual.*;

public class SmallPredicate {
    void a() {}

    void b() {}

    void d(@TestAccumulationPredicate("a && b") SmallPredicate this) {}

    static void test(SmallPredicate smallPredicate) {
        smallPredicate.a();
        smallPredicate.b();
        @TestAccumulation({"a", "b"}) SmallPredicate p2 = smallPredicate;
        smallPredicate.d();
    }
}
