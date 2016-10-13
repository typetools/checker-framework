import org.checkerframework.checker.nullness.Opt;
import org.checkerframework.checker.nullness.qual.*;

/** Test class org.checkerframework.checker.nullness.Opt. */
//@non-308-skip-test
class TestOpt {
    void foo(@Nullable Object p) {
        if (Opt.isPresent(p)) {
            p.toString(); // Flow refinement
        }
    }
}
