package checkers.nonnull.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import checkers.quals.PostconditionAnnotation;

/**
 * A postcondition annotation to indicate that certain expressions are
 * {@link NonNull} on successful termination of a method.
 *
 * @author Stefan Heule
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR })
@PostconditionAnnotation(annotation = NonNull.class)
public @interface EnsuresNonNull {
    /**
     * The Java expressions that are ensured to be {@link NonNull} on successful
     * method termination.
     *
     * @see <a
     *      href="http://types.cs.washington.edu/checker-framework/current/checkers-manual.html#java-expressions-as-arguments">Syntax
     *      of Java expressions</a>
     */
    String[] value();
}
