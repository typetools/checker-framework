package tests.util;

import checkers.quals.ConditionalPostconditionAnnotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A conditional postcondition annotation to indicate that a method ensures
 * certain expressions to be {@link Odd} given a certain result (either true or
 * false).
 *
 * @author Stefan Heule
 */
@ConditionalPostconditionAnnotation(qualifier = Odd.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR })
public @interface EnsuresOddIf {
    String[] expression();

    boolean result();
}
