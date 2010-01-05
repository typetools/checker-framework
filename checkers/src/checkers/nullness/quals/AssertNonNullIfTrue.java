package checkers.nullness.quals;

import java.lang.annotation.*;
import java.io.File;

import checkers.nullness.NullnessChecker;

/**
 * Indicates that if the method returns true, then the value expressions
 * are non-null.
 * <p>
 *
 * For instance, if {@link File#isDirectory()} is true, then {@link
 * File#list()} is non-null, and {@link File#listFiles()} is non-null.  You
 * can express this relationship as:
 *
 * <pre><code>   @AssertNonNullIfTrue({"list()","listFiles()"})
 *   public boolean isDirectory() { ... }
 * </code></pre>
 * <p>
 *
 * @see NonNull
 * @see NullnessChecker
 * @checker.framework.manual #nullness-checker Nullness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AssertNonNullIfTrue {

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
