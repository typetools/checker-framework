import org.checkerframework.common.value.qual.MinLen;

public class StateMatch {
    private int num_elts = 0;

    @SuppressWarnings("nullness")
    private double @MinLen(1) [][] elts = null;

    @SuppressWarnings("Interning")
    public boolean state_match(Object state) {
        if (!(state instanceof double[][])) {
            System.out.println("");
        }

        @SuppressWarnings("value") // This cast is inherently unsafe
        double @MinLen(1) [][] e = (double[][]) state;
        boolean match = false;
        if (elts[0] == e[0]) {
            // When analyzing this statement, we get an exception about taking
            // the LUB of ATMs with empty sets of qualifiers.
            match = true;
        }
        return (true);
    }
}
