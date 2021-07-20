package org.checkerframework.checker.nullness.util;

import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.AnnotatedFor;

import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Utility class for the Nullness Checker, providing every method in {@link java.util.Optional}, but
 * written for possibly-null references rather than for the {@code Optional} type.
 *
 * <p>To avoid the need to write the {@code Opt} class name at invocation sites, do:
 *
 * <pre>import static org.checkerframework.checker.nullness.util.Opt.orElse;</pre>
 *
 * or
 *
 * <pre>import static org.checkerframework.checker.nullness.util.Opt.*;</pre>
 *
 * <p><b>Runtime Dependency</b>: If you use this class, you must distribute (or link to) {@code
 * checker-qual.jar}, along with your binaries. Or, you can can copy this class into your own
 * project.
 *
 * @see java.util.Optional
 */
@AnnotatedFor("nullness")
public final class Opt {

    /** The Opt class cannot be instantiated. */
    private Opt() {
        throw new AssertionError("shouldn't be instantiated");
    }

    /**
     * If primary is non-null, returns it, otherwise throws NoSuchElementException.
     *
     * @param <T> the type of the argument
     * @param primary a non-null value to return
     * @return {@code primary} if it is non-null
     * @throws NoSuchElementException if primary is null
     * @see java.util.Optional#get()
     */
    // `primary` is @NonNull; otherwise, the method could throw an exception.
    public static <T extends @NonNull Object> T get(T primary) {
        if (primary == null) {
            throw new NoSuchElementException("No value present");
        }
        return primary;
    }

    /**
     * Returns true if primary is non-null, false if primary is null.
     *
     * @see java.util.Optional#isPresent()
     */
    @EnsuresNonNullIf(expression = "#1", result = true)
    public static boolean isPresent(@Nullable Object primary) {
        return primary != null;
    }

    /**
     * If primary is non-null, invoke the specified consumer with the value, otherwise do nothing.
     *
     * @see java.util.Optional#ifPresent(Consumer)
     */
    public static <T> void ifPresent(T primary, Consumer<@NonNull ? super @NonNull T> consumer) {
        if (primary != null) {
            consumer.accept(primary);
        }
    }

    /**
     * If primary is non-null, and its value matches the given predicate, return the value. If
     * primary is null or its non-null value does not match the predicate, return null.
     *
     * @see java.util.Optional#filter(Predicate)
     */
    public static <T> @Nullable T filter(
            T primary, Predicate<@NonNull ? super @NonNull T> predicate) {
        if (primary == null) {
            return null;
        } else {
            return predicate.test(primary) ? primary : null;
        }
    }

    /**
     * If primary is non-null, apply the provided mapping function to it and return the result. If
     * primary is null, return null.
     *
     * @see java.util.Optional#map(Function)
     */
    public static <T, U> @Nullable U map(
            T primary, Function<@NonNull ? super @NonNull T, ? extends U> mapper) {
        if (primary == null) {
            return null;
        } else {
            return mapper.apply(primary);
        }
    }

    // flatMap would have the same signature and implementation as map

    /**
     * Return primary if it is non-null. If primary is null, return other.
     *
     * @see java.util.Optional#orElse(Object)
     */
    public static <T> @NonNull T orElse(T primary, @NonNull T other) {
        return primary != null ? primary : other;
    }

    /**
     * Return primary if it is non-null. If primary is null, invoke {@code other} and return the
     * result of that invocation.
     *
     * @see java.util.Optional#orElseGet(Supplier)
     */
    public static <T> @NonNull T orElseGet(T primary, Supplier<? extends @NonNull T> other) {
        return primary != null ? primary : other.get();
    }

    /**
     * Return primary if it is non-null. If primary is null, throw an exception to be created by the
     * provided supplier.
     *
     * @see java.util.Optional#orElseThrow(Supplier)
     */
    // `primary` is @NonNull; otherwise, the method could throw an exception.
    public static <T extends @NonNull Object, X extends @NonNull Throwable> T orElseThrow(
            T primary, Supplier<? extends X> exceptionSupplier) throws X {
        if (primary != null) {
            return primary;
        } else {
            throw exceptionSupplier.get();
        }
    }
}
