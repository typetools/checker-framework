package checkers.igj.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * Indicates that the annotated reference is a ReadOnly reference.
 * 
 * A {@code ReadOnly} reference could refer to a Mutable or an Immutable
 * object. An object may not be mutated through a read only reference,
 * except if the field is marked {@code Assignable}. Only readonly method
 * can be called using a read only reference.
 *
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target ( { FIELD, LOCAL_VARIABLE, METHOD, PARAMETER, TYPE } )

public @interface ReadOnly {

}
