// Test case for Issue 185:
// https://github.com/typetools/kelloggm/issues/185

import java.util.BitSet;
import org.checkerframework.checker.index.qual.GTENegativeOne;

public class BitSetLowerBound {

    private void m(BitSet b) {
        b.set(b.nextClearBit(0));
        // next set bit does not have to exist
        // :: error: (argument.type.incompatible)
        b.clear(b.nextSetBit(0));
        @GTENegativeOne int i = b.nextSetBit(0);

        @GTENegativeOne int j = b.previousClearBit(-1);
        @GTENegativeOne int k = b.previousSetBit(-1);
    }
}
