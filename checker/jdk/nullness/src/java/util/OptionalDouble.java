package java.util;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

public class OptionalDouble {
    public static OptionalDouble empty() { throw new RuntimeException("skeleton method"); }
    public static OptionalDouble of(double arg0) { throw new RuntimeException("skeleton method"); }
    public double getAsDouble() { throw new RuntimeException("skeleton method"); }
    public boolean isPresent() { throw new RuntimeException("skeleton method"); }
    public void ifPresent(DoubleConsumer arg0) { throw new RuntimeException("skeleton method"); }
    public double orElse(double arg0) { throw new RuntimeException("skeleton method"); }
    public double orElseGet(DoubleSupplier arg0) { throw new RuntimeException("skeleton method"); }
    public <X extends Throwable> double orElseThrow(Supplier<X> arg0) throws X { throw new RuntimeException("skeleton method"); }
}
