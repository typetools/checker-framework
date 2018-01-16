package java.util.function;

import org.checkerframework.checker.lock.qual.GuardSatisfied;

public interface IntSupplier {
    int getAsInt(@GuardSatisfied IntSupplier this);
}
