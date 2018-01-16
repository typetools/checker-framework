package java.util.function;

import org.checkerframework.checker.lock.qual.GuardSatisfied;

public interface DoubleSupplier {
    double getAsDouble(@GuardSatisfied DoubleSupplier this);
}
