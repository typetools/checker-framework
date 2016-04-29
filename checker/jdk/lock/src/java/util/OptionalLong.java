package java.util;

import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public class OptionalLong {
    public static OptionalLong empty() { throw new RuntimeException("skeleton method"); }
    public static OptionalLong of(long arg0) { throw new RuntimeException("skeleton method"); }
    public long getAsLong() { throw new RuntimeException("skeleton method"); }
    public boolean isPresent() { throw new RuntimeException("skeleton method"); }
    public void ifPresent(LongConsumer arg0) { throw new RuntimeException("skeleton method"); }
    public long orElse(long arg0) { throw new RuntimeException("skeleton method"); }
    public long orElseGet(LongSupplier arg0) { throw new RuntimeException("skeleton method"); }
    public <X extends Throwable> long orElseThrow(Supplier<X> arg0) throws X { throw new RuntimeException("skeleton method"); }
}
