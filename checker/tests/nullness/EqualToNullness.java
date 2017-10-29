import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

public class EqualToNullness {

    //    @Nullable String f;
    //
    //    void t1(@Nullable String g) {
    //        // :: error: (dereference.of.nullable)
    //        g.toLowerCase();
    //        if (g != null) {
    //            g.toLowerCase();
    //        }
    //    }
    //
    //    void t2() {
    //        // :: error: (dereference.of.nullable)
    //        f.toLowerCase();
    //        if (f == null) {} else {
    //            f.toLowerCase();
    //        }
    //    }
    //
    //    void t1b(@Nullable String g) {
    //        // :: error: (dereference.of.nullable)
    //        g.toLowerCase();
    //        if (null != g) {
    //            g.toLowerCase();
    //        }
    //    }
    //
    //    void t2b() {
    //        // :: error: (dereference.of.nullable)
    //        f.toLowerCase();
    //        if (null == f) {} else {
    //            f.toLowerCase();
    //        }
    //    }
}
