// Tests whether the stub writer correctly handles named inner classes
// in anonymous classes.

import org.checkerframework.checker.testchecker.ainfer.qual.Sibling1;

public class NamedInnerClassInAnonymous {
    void test() {
        Object o =
                new NamedInnerClassInAnonymous() {
                    class NamedInner {
                        // The stub parser cannot parse inner classes, so stub-based WPI should
                        // not attempt to print a stub file for this.
                        public int mySibling1() {
                            return ((@Sibling1 int) 0);
                        }
                    }
                };
    }
}
