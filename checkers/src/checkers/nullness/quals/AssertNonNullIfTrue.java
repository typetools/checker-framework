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
    String[] value();
}
