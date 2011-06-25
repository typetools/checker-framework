package checkers.nullness.quals;

import java.lang.annotation.*;

import checkers.nullness.NullnessChecker;

/**
 * Indicates that if the method returns a non-null value, then the value
 * expressions are also non-null.
 * <p>
 *
 * Here is an example use::
 *
 * <pre><code>           @AssertNonNullIfNonNull("id")
 *     public @Pure @Nullable Long getId(){
 *         return id;
 *     }
 * </code></pre>
 *
 * You should <emph>not</em> write a formal parameter name or <tt>this</tt>
 * as the argument of this annotation.  In those cases, use the {@link
 * PolyNull} annotation instead.
 *
 * @see NonNull
 * @see PolyNull
 * @see NullnessChecker
 * @checker.framework.manual #nullness-checker Nullness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AssertNonNullIfNonNull {

    /**
     * Java expression(s) that are non-null after the method returns a non-null vlue.
     * @see <a href="http://types.cs.washington.edu/checker-framework/current/checkers-manual.html#java-expressions-as-arguments">Syntax of Java expressions</a>
     */
    String[] value();
}
