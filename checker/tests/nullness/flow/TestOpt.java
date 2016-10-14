import org.checkerframework.checker.nullness.Opt;
import org.checkerframework.checker.nullness.qual.*;

/** Test class org.checkerframework.checker.nullness.Opt. */
// @below-java8-jdk-skip-test
class TestOpt {
    void foo1(@Nullable Object p) {
        if (Opt.isPresent(p)) {
            p.toString(); // Flow refinement
        }
    }

    void foo2(@Nullable Object p) {
        if (!Opt.isPresent(p)) {
            //:: error: (dereference.of.nullable)
            p.toString();
        }
    }

    void foo3(@Nullable Object p) {
        Opt.ifPresent(p, x -> System.out.println("Got: " + x));
    }

    void foo4(@Nullable Object p) {
        // TODO: we should infer from ifPresent that x is non-null.
        //:: error: (dereference.of.nullable)
        Opt.ifPresent(p, x -> System.out.println("Got: " + x.toString()));
    }
}
