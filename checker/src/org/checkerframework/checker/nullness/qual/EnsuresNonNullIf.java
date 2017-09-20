package org.checkerframework.checker.nullness.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.ConditionalPostconditionAnnotation;
import org.checkerframework.framework.qual.InheritedAnnotation;

/**
 * Indicates that the given expressions are non-null, if the method returns the given result (either
 * true or false).
 *
 * <p>Here are ways this conditional postcondition annotation can be used:
 *
 * <p><b>Method parameters:</b> A common example is that the {@code equals} method is annotated as
 * follows:
 *
 * <pre>{@code   @EnsuresNonNullIf(expression="#1", result=true)
 *   public boolean equals(@Nullable Object obj) { ... }}</pre>
 *
 * because, if {@code equals} returns true, then the first (#1) argument to {@code equals} was not
 * null.
 *
 * <p><b>Fields:</b> The value expressions can refer to fields, even private ones. For example:
 *
 * <pre>{@code   @EnsuresNonNullIf(expression="this.derived", result=true)
 *   public boolean isDerived() {
 *     return (this.derived != null);
 *   }}</pre>
 *
 * As another example, an {@code Iterator} may cache the next value that will be returned, in which
 * case its {@code hasNext} method could be annotated as:
 *
 * <pre>{@code   @EnsuresNonNullIf(expression="next_cache", result=true)
 *   public boolean hasNext() {
 *     if (next_cache == null) return false;
 *     ...
 *   }}</pre>
 *
 * An {@code EnsuresNonNullIf} annotation that refers to a private field is useful for verifying
 * that client code performs needed checks in the right order, even if the client code cannot
 * directly affect the field.
 *
 * <p><b>Method calls:</b> If {@link Class#isArray()} returns true, then {@link
 * Class#getComponentType()} returns non-null. You can express this relationship as:
 *
 * <pre>{@code   @EnsuresNonNullIf(expression="getComponentType()", result=true)
 *   public native @Pure boolean isArray();}</pre>
 *
 * <!-- Issue:  https://tinyurl.com/cfissue/1307 -->
 * You cannot write two {@code @EnsuresNonNullIf} annotations on a single method; to get the effect
 * of
 *
 * <pre><code>
 * &nbsp;   @EnsuresNonNullIf(expression="outputFile", result=true)
 * &nbsp;   @EnsuresNonNullIf(expression="memoryOutputStream", result=false)
 *     public boolean isThresholdExceeded() { ... }
 * </code></pre>
 *
 * you need to instead write
 *
 * <pre><code>
 * &nbsp;@EnsuresQualifiersIf({
 * &nbsp;  @EnsuresQualifierIf(result=true, qualifier=NonNull.class, expression="outputFile"),
 * &nbsp;  @EnsuresQualifierIf(result=false, qualifier=NonNull.class, expression="memoryOutputStream")
 * })
 * </code></pre>
 *
 * @see NonNull
 * @see EnsuresNonNull
 * @see org.checkerframework.checker.nullness.NullnessChecker
 * @checker_framework.manual #nullness-checker Nullness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@ConditionalPostconditionAnnotation(qualifier = NonNull.class)
@InheritedAnnotation
public @interface EnsuresNonNullIf {
    /**
     * Java expression(s) that are non-null after the method returns the given result.
     *
     * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
     */
    String[] expression();

    /** The return value of the method that needs to hold for the postcondition to hold. */
    boolean result();
}
