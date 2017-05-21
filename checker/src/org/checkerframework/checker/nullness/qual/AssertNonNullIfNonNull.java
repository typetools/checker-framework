package org.checkerframework.checker.nullness.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that if the method returns a non-null value, then the value expressions are also
 * non-null.
 *
 * <p><b>WARNING:</b> Type-checking for this annotation is <em>not implemented</em> at present.
 *
 * <p>Here is an example use:
 *
 * <pre><code>
 *    {@literal @}AssertNonNullIfNonNull("id")
 *    {@literal @}Pure
 *     public @Nullable Long getId() {
 *         return id;
 *     }
 * </code></pre>
 *
 * Note the direction of the implication. This annotation says that if the result is non-null, then
 * the variable {@code id} is also non-null. The annotation does not say that if {@code id} is
 * non-null, then the result is non-null. (There is not currently a way to say the latter, though it
 * would also be useful.)
 *
 * <p>You should <em>not</em> write a formal parameter name or {@code this} as the argument of this
 * annotation. In those cases, use the {@link PolyNull} annotation instead.
 *
 * @see NonNull
 * @see PolyNull
 * @see org.checkerframework.checker.nullness.NullnessChecker
 * @checker_framework.manual #nullness-checker Nullness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AssertNonNullIfNonNull {

    /**
     * Java expression(s) that are non-null after the method returns a non-null vlue.
     *
     * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
     */
    String[] value();
}
