package org.checkerframework.framework.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that the field may not be accessed if the receiver is of the specified qualifier type
 * (or any supertype).
 *
 * <p>This property is verified by the checker that type-checks the {@code when} element value
 * qualifier.
 *
 * <p><b>Example</b> Consider a class, {@code Table}, with a locking field, {@code lock}. The lock
 * is used when a {@code Table} instance is shared across threads. When running in a local thread,
 * the {@code lock} field ought not to be used.
 *
 * <p>You can declare this behavior in the following way:
 *
 * <pre>{@code
 * class Table {
 *   private @Unused(when=LocalToThread.class) final Lock lock;
 *   ...
 * }
 * }</pre>
 *
 * The checker for {@code @LocalToThread} would issue an error for the following code:
 *
 * <pre>  @LocalToThread Table table = ...;
 *   ... table.lock ...;
 * </pre>
 *
 * @checker_framework.manual #unused-fields Unused fields
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Unused {
    /**
     * The field that is annotated with @Unused may not be accessed via a receiver that is annotated
     * with the "when" annotation.
     */
    Class<? extends Annotation> when();
}
