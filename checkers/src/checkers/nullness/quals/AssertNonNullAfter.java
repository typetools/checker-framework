package checkers.nullness.quals;

import java.lang.annotation.*;
import java.io.File;

import checkers.nullness.NullnessChecker;

/**
 * Indicates that if the method terminates successfully, the value expressions
 * are non-null.
 * <p>
 *
 * @see NonNull
 * @see NullnessChecker
 * @checker.framework.manual #nullness-checker Nullness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AssertNonNullAfter {

    /**
     * The value can be:
     *
     * <ol>
     * <li>fields on receiver object.  The value should simply be the
     * field name, e.g. {@code next}, {@code parent}.
     *
     * <li>no-arg method members on the receiver object:  The value
     * would be the method signature, e.g. {@code list()}
     *
     * <li>method argument:  The value should be {@code #} followed
     * by the parameter index (index starts with 0), e.g. {@code #2}.
     * </ol>
     */
    String[] value();
}
