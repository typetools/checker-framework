import testlib.flowexpression.qual.FlowExp;

public class Issue2613 {
    public static final int ONE = 1;
    public static final double ONE_0 = 1.0;

    @FlowExp("ONE") String one;

    @FlowExp("ONE_0") String one_0;

    void method(double d) {
        @FlowExp("1") String x0 = one;
        @FlowExp("ONE") String x1 = one;
        @FlowExp("ONE_0") String x4 = one_0;
        // :: error: (expression.unparsable.type.invalid) :: error: (assignment.type.incompatible)
        @FlowExp("1.0") String x5 = one_0;
    }
}
