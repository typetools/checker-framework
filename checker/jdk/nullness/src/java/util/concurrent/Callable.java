package java.util.concurrent;

import org.checkerframework.checker.nullness.qual.Nullable;

@FunctionalInterface
// Make upper bound explicit for clarity.
public interface Callable<V extends @Nullable Object> {
    V call() throws Exception;
}
