// Test case from
// http://stackoverflow.com/questions/38339332/in-a-bounded-wildcard-where-does-the-annotation-belong

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

class Styleable {}

public class BoundedWildcardTest {

    private void locChildren(Styleable c) {
        // ...
    }

    public void initLoc(List<? extends Styleable> s) {
        for (Styleable c : s) {
            locChildren(c);
        }
    }

    // :: error: (bound.type.incompatible)
    public void initLoc1(@Nullable List<@Nullable ? extends Styleable> s) {
        // :: error: (iterating.over.nullable)
        for (Styleable c : s) {
            locChildren(c);
        }
    }

    public void initLoc2(@Nullable List<@Nullable ? extends @Nullable Styleable> s) {
        // :: error: (iterating.over.nullable)
        for (Styleable c : s) {
            // :: error: argument.type.incompatible
            locChildren(c);
        }
    }

    public void initLoc3(@Nullable List<? extends @Nullable Styleable> s) {
        // :: error: (iterating.over.nullable)
        for (Styleable c : s) {
            // :: error: argument.type.incompatible
            locChildren(c);
        }
    }
}
