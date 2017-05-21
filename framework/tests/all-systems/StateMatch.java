
@SuppressWarnings("array.access.unsafe.high") // The Index Checker correctly issues a warning.
public class StateMatch {
    private int num_elts = 0;

    @SuppressWarnings("nullness")
    private double[][] elts = null;

    @SuppressWarnings("Interning")
    public boolean state_match(Object state) {
        if (!(state instanceof double[][])) {
            System.out.println("");
        }

        double[][] e = (double[][]) state;
        boolean match = false;
        if (elts[0] == e[0]) {
            // When analyzing this statement, we get an exception about taking
            // the LUB of ATMs with empty sets of qualifiers.
            match = true;
        }
        return (true);
    }
}
