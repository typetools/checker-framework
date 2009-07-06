package checkers.nullness.quals;

import java.lang.annotation.*;

import checkers.nullness.NullnessChecker;

/**
 * Indicates that the method throws a {@link Throwable} if any of its
 * parameters is a {@code null}.
 *
 * @see NonNull
 * @see NullnessChecker
 * @manual #nullness-checker Nullness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AssertNonNull {
}
