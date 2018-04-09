import org.checkerframework.checker.index.qual.LTLengthOf;

public class NotEnoughOffsets {

    int[] a;
    int[] b;
    int c;

    // :: error: (not.enough.offsets)
    void badParam(
            @LTLengthOf(
                        value = {"a", "b"},
                        offset = {"c"}
                    )
                    int x) {}
}
