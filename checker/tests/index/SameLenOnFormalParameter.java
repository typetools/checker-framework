// Test case for issue #2434: http://tinyurl.com/cfissue/2434

import org.checkerframework.checker.index.qual.*;

class SameLenOnFormalParameter {
    public void requiresSameLen1(String x1, @SameLen("#1") String y1) {}

    public void requiresSameLen2(@SameLen("#2") String x2, String y2) {}

    public void m1(@SameLen("#2") String a1, String b1) {
        requiresSameLen1(a1, b1);
    }

    public void m2(@SameLen("#2") String a2, String b2) {
        @SameLen("a2") String b22 = b2;
        requiresSameLen1(a2, b22);
    }

    public void m3(@SameLen("#2") String a3, String b3) {
        @SameLen("b3") String a2 = a3;
        @SameLen("a3") String b32 = b3;
        requiresSameLen1(a3, b32);
    }

    public void m4(@SameLen("#2") String a4, String b4) {
        requiresSameLen2(a4, b4);
    }
}
