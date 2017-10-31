// Test case for Issue 166:
// https://github.com/kelloggm/checker-framework/issues/166

import org.checkerframework.checker.index.qual.IndexFor;

public class Index166 {

    public void testMethodInvocation() {
        requiresIndex("012345", 5);
        // :: error: (argument.type.incompatible)
        requiresIndex("012345", 6);
    }

    public void requiresIndex(String str, @IndexFor("#1") int index) {}
}
