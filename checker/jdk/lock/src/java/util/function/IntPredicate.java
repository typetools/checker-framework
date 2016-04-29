package java.util.function;

public interface IntPredicate {
    boolean test(int arg0);
    IntPredicate and(IntPredicate arg0);
    IntPredicate negate();
    IntPredicate or(IntPredicate arg0);
}
