package tests.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import checkers.quals.ConditionalPostconditionAnnotation;

/**
 * A conditional postcondition annotation to indicate that a method ensures
 * certain expressions to be {@link Odd} given a certain result (either true or
 * false).
 *
 * @author Stefan Heule
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR })
@ConditionalPostconditionAnnotation(qualifier = Odd.class)
public @interface EnsuresOddIf {
    String[] expression();

    boolean result();
}
