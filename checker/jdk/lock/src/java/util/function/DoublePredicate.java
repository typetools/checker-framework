package java.util.function;

public interface DoublePredicate {
    boolean test(double arg0);
    DoublePredicate and(DoublePredicate arg0);
    DoublePredicate negate();
    DoublePredicate or(DoublePredicate arg0);
}
