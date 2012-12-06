
public class StateMatch {
    private int num_elts = 0;
    private double[][] elts = null;

    public boolean state_match (Object state) {
        if (!(state instanceof double [][]))
            System.out.println ("");

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