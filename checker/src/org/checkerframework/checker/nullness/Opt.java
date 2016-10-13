package org.checkerframework.checker.nullness;

/*>>>
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
*/

import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;

/**
 * Utilities class for the Nullness Checker, providing methods similar
 * to java.util.Optional.
 * <p>
 *
 * To avoid the need to write the Opt class name, do:
 * <pre>import static org.checkerframework.checker.nullness.Opt.orElse;</pre>
 * or
 * <pre>import static org.checkerframework.checker.nullness.Opt.*;</pre>
 * <p>
 *
 * <b>Runtime Dependency</b>
 * <p>
 *
 * Please note that using this class introduces a Runtime dependency.
 * This means that you need to distribute (or link to) the Checker
 * Framework, along with your binaries.
 *
 * To eliminate this dependency, you can simply copy this class into your
 * own project.
 *
 * @see java.util.Optional
 */
public final class Opt {

    private Opt() {
        throw new AssertionError("shouldn't be instantiated");
    }

    public static <T extends /*@Nullable*/ Object> /*@NonNull*/ T get(/*@Nullable*/ T primary) {
        if (primary == null) {
            throw new NoSuchElementException("No value present");
        }
        return primary;
    }

    @EnsuresNonNullIf(expression = "#1", result = true)
    public boolean isPresent(/*@Nullable*/ Object primary) {
        return primary != null;
    }

    public static <T extends /*@Nullable*/ Object> void ifPresent(
            T primary, Consumer<? super T> consumer) {
        if (primary != null) {
            consumer.accept(primary);
        }
    }

    public static <T extends /*@Nullable*/ Object> /*@Nullable*/ T filter(
            T primary, Predicate<? super T> predicate) {
        if (primary == null) {
            return null;
        } else {
            return predicate.test(primary) ? primary : null;
        }
    }

    public static <T extends /*@Nullable*/ Object, U extends /*@Nullable*/ Object>
            /*@Nullable*/ U map(T primary, Function<? super T, ? extends U> mapper) {
        if (primary == null) {
            return null;
        } else {
            return mapper.apply(primary);
        }
    }

    // flatMap would have the same signature and implementation as map

    public static <T extends /*@Nullable*/ Object> /*@NonNull*/ T orElse(
            T primary, /*@NonNull*/ T other) {
        return primary != null ? primary : other;
    }

    public static <T extends /*@Nullable*/ Object> /*@NonNull*/ T orElseGet(
            T primary, Supplier<? extends /*@NonNull*/ T> other) {
        return primary != null ? primary : other.get();
    }

    public static <T extends /*@Nullable*/ Object, X extends /*@NonNull*/ Throwable>
            /*@NonNull*/ T orElseThrow(
            T primary, Supplier<? extends /*@NonNull*/ X> exceptionSupplier) throws X {
        if (primary != null) {
            return primary;
        } else {
            throw exceptionSupplier.get();
        }
    }
}
