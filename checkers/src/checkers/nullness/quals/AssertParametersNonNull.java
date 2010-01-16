package checkers.nullness.quals;

import java.lang.annotation.*;

import checkers.nullness.NullnessChecker;

/**
 * Indicates that the method throws an exception if any of its
 * parameters is {@code null}.
 * <p>
 *
 * Use of this annotation should be very rare.  In most cases, it is better
 * to simply annotate each parameter as {@link NonNull}.
 * <p>
 *
 * This annotation is intended for use only on methods whose sole purpose
 * is suppressing warnings, not on methods that are used for defensive
 * programming.  See the Checker Framework manual for more details.
 *
 * @see NonNull
 * @see NullnessChecker
 * @checker.framework.manual #defensive-programming Nullness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AssertParametersNonNull {
}
