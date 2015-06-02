package java.util.function;

public interface LongPredicate {
    boolean test(long arg0);
    LongPredicate and(LongPredicate arg0);
    LongPredicate negate();
    LongPredicate or(LongPredicate arg0);
}
