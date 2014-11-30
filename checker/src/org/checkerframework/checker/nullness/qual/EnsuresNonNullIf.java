package org.checkerframework.checker.nullness.qual;

import org.checkerframework.framework.qual.ConditionalPostconditionAnnotation;
import org.checkerframework.framework.qual.InheritedAnnotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the given expressions are non-null,
 * if the method returns the given result (either true or false).
 * <p>
 *
 * Here are ways this conditional postcondition annotation can be used:
 * <p>
 *
 * <b>Method parameters:</b>
 * A common example is that the <tt>equals</tt> method is annotated as follows:
 * <pre><code>   @EnsuresNonNullIf(expression="#1", result=true)
 *   public boolean equals(@Nullable Object obj) { ... }</code></pre>
 * because, if <tt>equals</tt> returns true, then the first (#1) argument to
 * <tt>equals</tt> was not null.
 * <p>
 *
 * <b>Fields:</b>
 * The value expressions can refer to fields, even private ones.  For example:
 * <pre><code>   @EnsuresNonNullIf(expression="this.derived", result=true)
 *   public boolean isDerived() {
 *     return (this.derived != null);
 *   }</code></pre>
 * As another example, an <tt>Iterator</tt> may cache the next value that
 * will be returned, in which case its <tt>hasNext</tt> method could be
 * annotated as:
 * <pre><code>   @EnsuresNonNullIf(expression="next_cache", result=true)
 *   public boolean hasNext() {
 *     if (next_cache == null) return false;
 *     ...
 *   }</code></pre>
 * An <tt>EnsuresNonNullIf</tt> annotation that refers to a private field is
 * useful for verifying that client code performs needed checks in the right
 * order, even if the client code cannot directly affect the field.
 * <p>
 *
 * <b>Method calls:</b>
 * If {@link Class#isArray()} returns true, then {@link Class#getComponentType()}
 * returns non-null.  You can express this relationship as:
 * <pre><code>   @EnsuresNonNullIf(expression="getComponentType()", result=true)
 *   public native @Pure boolean isArray();</code></pre>
 *
 * @see NonNull
 * @see EnsuresNonNull
 * @see org.checkerframework.checker.nullness.NullnessChecker
 * @checker_framework.manual #nullness-checker Nullness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR })
@ConditionalPostconditionAnnotation(qualifier = NonNull.class)
@InheritedAnnotation
public @interface EnsuresNonNullIf {
    /**
     * Java expression(s) that are non-null after the method returns the
     * given result.
     * @see <a href="http://types.cs.washington.edu/checker-framework/current/checker-framework-manual.html#java-expressions-as-arguments">Syntax of Java expressions</a>
     */
    String[] expression();

    /**
     * The return value of the method that needs to hold for the postcondition
     * to hold.
     */
    boolean result();
}
