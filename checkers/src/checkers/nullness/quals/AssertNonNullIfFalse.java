package checkers.nullness.quals;

import java.lang.annotation.*;
import java.util.PriorityQueue;

import checkers.nullness.NullnessChecker;

/**
 * Indicates that if the method returns false, then the value expressions
 * are non-null.
 * <p>
 *
 * For instance, if {@link PriorityQueue#isEmpty()} is false, then
 * {@link PriorityQueue#peek()} is nonnull.  You can express
 * this relationship as:
 *
 * <pre><code>  @AssertNonNullIfFalse({"peek()"})
 *   public boolean isEmpty() { ... }
 * </code></pre>
 * <p>
 *
 * @see NonNull
 * @see AssertNonNullIfTrue
 * @see NullnessChecker
 * @checker.framework.manual #nullness-checker Nullness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AssertNonNullIfFalse {

    /**
     * Java expression(s) that are non-null after the method returns false.
     * @see <a href="http://types.cs.washington.edu/checker-framework/#java-expressions-as-arguments">Syntax of Java expressions</a>
     */
    String[] value();
}
