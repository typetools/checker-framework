package java.util.function;

import org.checkerframework.checker.lock.qual.GuardSatisfied;

public interface Supplier<T> {
    T get(@GuardSatisfied Supplier<T> this);
}
