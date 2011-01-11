package checkers.nullness.quals;

import java.lang.annotation.*;
import java.io.File;

import checkers.nullness.NullnessChecker;

/**
 * Indicates that if the method terminates successfully, the value expressions
 * are non-null.
 * <p>
 *
 * This is useful for methods that initialize a field, for example.
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
     * Java expression(s) that are non-null after successful method termination.
     * @see <a href="http://types.cs.washington.edu/checker-framework/#java-expressions-as-arguments">Syntax of Java expressions</a>
     */
    String[] value();
}
