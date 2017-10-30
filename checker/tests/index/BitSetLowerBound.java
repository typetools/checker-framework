import java.util.BitSet;

public class BitSetLowerBound {

    private void m(BitSet b) {
        b.set(b.nextClearBit(0));
        // next set bit does not have to exist
        // :: argument.type.incompatible
        b.clear(b.nextSetBit(0));
        @GTENegativeOne int i = b.nextSetBit(0);
    }
}
