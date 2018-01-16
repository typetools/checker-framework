package java.util.function;

public interface Supplier<T> {
    T get(@GuardSatisfied Supplier<T> this);
}
