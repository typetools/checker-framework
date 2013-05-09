package checkers.nullness.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import checkers.quals.ConditionalPostconditionAnnotation;

/**
 * A conditional postcondition annotation to indicate that a method ensures
 * certain expressions to be {@link NonNull} given a certain result (either true
 * or false).
 *
 * @author Stefan Heule
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR })
@ConditionalPostconditionAnnotation(qualifier = NonNull.class)
public @interface EnsuresNonNullIf {
    /**
     * The Java expressions that are ensured to be {@link NonNull} on successful
     * method termination.
     *
     * @see <a
     *      href="http://types.cs.washington.edu/checker-framework/current/checkers-manual.html#java-expressions-as-arguments">Syntax
     *      of Java expressions</a>
     */
    String[] expression();

    /**
     * The return value of the method that needs to hold for the postcondition
     * to hold.
     */
    boolean result();
}
