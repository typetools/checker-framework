package checkers.oigj.quals;

import java.lang.annotation.*;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

import static java.lang.annotation.ElementType.*;

/**
 * Indicates that the annotated reference is a ReadOnly reference.
 *
 * A {@code ReadOnly} reference could refer to a Mutable or an Immutable
 * object. An object may not be mutated through a read only reference,
 * except if the field is marked {@code Assignable}. Only a method with a
 * readonly receiver can be called using a readonly reference.
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
//@Target ( { FIELD, LOCAL_VARIABLE, METHOD, PARAMETER, TYPE } )
@TypeQualifier
@SubtypeOf( { } )
public @interface ReadOnly {

}
