public class StateMatch {
    private int num_elts = 0;

    @SuppressWarnings("nullness")
    private double[][] elts = null;

    @SuppressWarnings({
        "Interning",
        "index"
    }) // This code is inherently unsafe for the index checker, but adding index annotations
    // produces warnings for other checkers (fenum)
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
