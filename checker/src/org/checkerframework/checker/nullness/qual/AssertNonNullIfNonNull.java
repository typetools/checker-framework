package org.checkerframework.checker.nullness.qual;

import java.lang.annotation.*;

import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.PolyNull;

/**
 * Indicates that if the method returns a non-null value, then the value
 * expressions are also non-null.
 * <p>
 *
 * <b>WARNING:</b>
 * Type-checking for this annotation is <em>not implemented</em> at present.
 * <p>
 *
 * Here is an example use:
 *
 * <pre><code>     @AssertNonNullIfNonNull("id")
 *     {@literal @}Pure
 *     public @Nullable Long getId(){
 *         return id;
 *     }
 * </code></pre>
 *
 * Note the direction of the implication.  This annotation says that if the
 * result is non-null, then the variable <tt>id</tt> is also non-null.  The
 * annotation does not say that if <tt>id</tt> is non-null, then the result
 * is non-null.  (There is not currently a way to say the latter,
 * though it would also be useful.)
 * <p>
 *
 * You should <em>not</em> write a formal parameter name or <tt>this</tt>
 * as the argument of this annotation.  In those cases, use the {@link
 * PolyNull} annotation instead.
 *
 * @see NonNull
 * @see PolyNull
 * @see NullnessChecker
 * @checker_framework.manual #nullness-checker Nullness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AssertNonNullIfNonNull {

    /**
     * Java expression(s) that are non-null after the method returns a non-null vlue.
     * @see <a href="http://types.cs.washington.edu/checker-framework/current/checker-framework-manual.html#java-expressions-as-arguments">Syntax of Java expressions</a>
     */
    String[] value();
}
