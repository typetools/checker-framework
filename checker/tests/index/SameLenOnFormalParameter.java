// Test case for issue #2434: http://tinyurl.com/cfissue/2434

// @skip-test until the bug is fixed

import org.checkerframework.checker.index.qual.*;

class SameLenOnFormalParameter {
    public void requiresSameLen1(String x, @SameLen("#1") String y) {}

    public void requiresSameLen2(@SameLen("#2") String x, String y) {}

    public void m1(@SameLen("#2") String a, String b) {
        requiresSameLen1(a, b);
    }

    public void m2(@SameLen("#2") String a, String b) {
        @SameLen("a") String b2 = b;
        requiresSameLen1(a, b2);
    }

    public void m3(@SameLen("#2") String a, String b) {
        @SameLen("b") String a2 = a;
        @SameLen("a") String b2 = b;
        requiresSameLen1(a, b2);
    }

    public void m4(@SameLen("#2") String a, String b) {
        requiresSameLen2(a, b);
    }
}
