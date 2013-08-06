package checkers.igj.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * Indicates that the annotated reference is an immutable reference to an
 * immutable object.
 * 
 * An Immutable object cannot be modified. Its fields may be reassigned or
 * mutated only if they are explicitly marked as Mutable or Assignable.
 * 
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target( { FIELD, LOCAL_VARIABLE, CONSTRUCTOR, METHOD, PARAMETER, TYPE })

public @interface Mutable {

}