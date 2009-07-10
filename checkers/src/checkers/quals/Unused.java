package checkers.quals;

import static java.lang.annotation.ElementType.*;

import java.lang.annotation.*;

/**
 * Declares that the field may not be accessed if the receiver is of the
 * specified qualifier type.
 *
 * The checker that type-checks the {@code when} element value qualifier
 * verifies that property.
 *
 * <p><b>Example</b>
 * Consider a class, {@code Table}, with a locking field, {@code lock}.  The
 * lock is used when a {@code Table} instance is shared across threads.  When
 * running in a local thread, the {@code lock} field ought not to be used.
 *
 * You can declare this behavior in the following way:
 *
 * <pre><code>
 * class Table {
 *   private @Unused(when=LocalToThread.class) final Lock lock;
 *   ...
 * }
 * </code></pre>
 *
 * The appropriate checker (which type-checks the specified qualifier) is to
 * issue an error whenever the lock is used in a {@code LocalToThread}, like
 * in the following case:
 *
 * <pre><code>
 *   final @LocalToThread Table table = ....;
 *   table.lock.lock();
 * </code></pre>
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({FIELD})
public @interface Unused {
    Class<? extends Annotation> when();
}
