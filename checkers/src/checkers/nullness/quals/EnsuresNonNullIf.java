package checkers.nullness.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import checkers.quals.ConditionalPostconditionAnnotation;

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
 * <pre><code>   @AssertNonNullIfTrue("#1")
 *   public boolean equals(@Nullable Object obj) { ... }</code></pre>
 * because, if <tt>equals</tt> returns true, then the first (#0) argument to
 * <tt>equals</tt> was not null.
 * <p>
 *
 * <b>Fields:</b>
 * The value expressions can refer to fields, even private ones.  For example:
 * <pre><code>   @AssertNonNullIfTrue("this.derived")
 *   public boolean isDerived() {
 *     return (this.derived != null);
 *   }</code></pre>
 * As another example, an <tt>Iterator</tt> may cache the next value that
 * will be returned, in which case its <tt>hasNext</tt> method could be
 * annotated as:
 * <pre><code>   @AssertNonNullIfTrue("next_cache")
 *   public boolean hasNext() {
 *     if (next_cache == null) return false;
 *     ...
 *   }</code></pre>
 * An <tt>AssertNonNullIfTrue</tt> annotation that refers to a private field is
 * useful for verifying that client code performs needed checks in the right
 * order, even if the client code cannot directly affect the field.
 * <p>
 *
 * <b>Method calls:</b>
 * If {@link File#isDirectory()} returns true, then {@link File#list()} returns
 * non-null, and {@link File#listFiles()} returns non-null.  You
 * can express this relationship as:
 * <pre><code>   @AssertNonNullIfTrue({"list()","listFiles()"})
 *   public boolean isDirectory() { ... }</code></pre>
 *
 * @see NonNull
 * @see AssertNonNullIfFalse
 * @see NullnessChecker
 * @checker.framework.manual #nullness-checker Nullness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR })
@ConditionalPostconditionAnnotation(qualifier = NonNull.class)
public @interface EnsuresNonNullIf {
    /**
     * Java expression(s) that are non-null after the method returns the
     * given result.
     * @see <a href="http://types.cs.washington.edu/checker-framework/current/checkers-manual.html#java-expressions-as-arguments">Syntax of Java expressions</a>
     */
    String[] expression();

    /**
     * The return value of the method that needs to hold for the postcondition
     * to hold.
     */
    boolean result();
}
