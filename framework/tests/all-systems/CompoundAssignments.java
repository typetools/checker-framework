class CompoundAssignments {
    static final int SIZE = 4;

    // There used to be a bug creating the LeftShiftAssignmentNode
    // where the target (e.g. pow) was replaced by the shift amount
    // (e.g. 1).
    static void left_shift_assign() {
        for (int i = 0, pow = 1; i <= SIZE; i++) {
            pow <<= 1;
        }
    }

    // There used to be a bug computing the Receiver for a widening
    // conversion, such as widening sum to a double below.
    static int sum_with_widening() {
        double[] freq = new double[SIZE];
        int sum = 0;
        for (int i = 0; i < SIZE; i++) {
            sum += freq[i];
        }
        return sum;
    }
}
