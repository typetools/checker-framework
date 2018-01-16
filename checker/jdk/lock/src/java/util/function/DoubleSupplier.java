package java.util.function;

public interface DoubleSupplier {
    double getAsDouble(@GuardSatisfied DoubleSupplier this);
}
