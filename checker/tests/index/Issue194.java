// Test case for kelloggm 194
// https://github.com/kelloggm/checker-framework/issues/194

import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.LengthOf;
import org.checkerframework.checker.index.qual.SameLen;

public class Issue194 {
    class Custom {
        public @LengthOf("this") int length() {
            throw new RuntimeException();
        }

        public Object get(@IndexFor("this") int i) {
            return null;
        }

        void call() {
            length();
        }
    }

    public boolean m(Custom a, Custom b) {
        if (a.length() != b.length()) {
            return false;
        }
        for (int i = 0; i < a.length(); ++i) {
            if (a.get(i) != b.get(i)) {
                return false;
            }
        }
        return true;
    }

    public void m2(Custom a, Custom b) {
        if (a.length() != b.length()) {
            return;
        }
        @SameLen("a") Custom a2 = b;
        @SameLen("b") Custom b2 = a;
    }
}
