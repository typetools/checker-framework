package java.util;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class OptionalInt {
    public static OptionalInt empty() { throw new RuntimeException("skeleton method"); }
    public static OptionalInt of(int arg0) { throw new RuntimeException("skeleton method"); }
    public int getAsInt() { throw new RuntimeException("skeleton method"); }
    public boolean isPresent() { throw new RuntimeException("skeleton method"); }
    public void ifPresent(IntConsumer arg0) { throw new RuntimeException("skeleton method"); }
    public int orElse(int arg0) { throw new RuntimeException("skeleton method"); }
    public int orElseGet(IntSupplier arg0) { throw new RuntimeException("skeleton method"); }
    public <X extends Throwable> int orElseThrow(Supplier<X> arg0) throws X { throw new RuntimeException("skeleton method"); }
}
