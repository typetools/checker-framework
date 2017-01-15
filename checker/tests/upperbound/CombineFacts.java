import org.checkerframework.checker.upperbound.qual.*;

class CombineFacts {
    void test(int[] a1) {
        @LTLengthOf("a1") int len = a1.length - 1;
        int[] a2 = new int[len];
        // At this point, dataflow knows that len is less than a1.length and less than
        // or equal to a2.length, but there is not annotation to represent that
        // knowledge. So dataflow attempts to refine the type of len to
        // @LTEqLengthOf("a1", "a1").  However this is not a subtype of the declared
        // type of len, so the refine is not applied.  This causes the false positive
        // warning below:
        //:: error: (array.access.unsafe.high)
        a2[len - 1] = 1;
        // This access is still legal.
        a1[len] = 1;

        // This access should issue an error.
        //:: error: (array.access.unsafe.high)
        a2[len] = 1;
    }
}
