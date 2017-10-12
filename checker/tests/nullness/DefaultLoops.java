import java.util.LinkedList;
import org.checkerframework.checker.nullness.qual.*;

class MyTS extends LinkedList {}

public class DefaultLoops {
    void m() {
        MyTS ts = new MyTS();
        // s should default to @Nullable
        for (Object s : ts) {}
    }

    void bar() {
        for (int i = 0; i < 100; ++i) {
            // nullable by default
            Object o;
            o = null;
            // :: error: (dereference.of.nullable)
            o.hashCode();
            o = new Object();
            o.hashCode();
        }
        for (int i = 0; i < 100; ++i) {
            // nullable by default
            Object o;
            o = new Object();
            o.hashCode();
        }
        int i = 0;
        // nullable by default
        for (Object o2; i < 100; ++i) {
            o2 = null;
            int i3 = new Object().hashCode();
        }
    }
}
