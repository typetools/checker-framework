import org.checkerframework.checker.index.qual.*;
import org.checkerframework.common.value.qual.*;

class BottomValTest {
    @NonNegative int foo(@BottomVal int bottom) {
        return bottom;
    }

    @Positive int bar(@BottomVal int bottom) {
        return bottom;
    }

    @LTLengthOf("#1") int baz(int[] a, @BottomVal int bottom) {
        return bottom;
    }
}
