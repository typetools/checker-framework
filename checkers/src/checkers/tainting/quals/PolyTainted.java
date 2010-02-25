package checkers.tainting.quals;

import static java.lang.annotation.ElementType.*;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * A polymorphic qualifier for the Tainting type system.
 *
 */
@Documented
@TypeQualifier
@PolymorphicQualifier
@Retention(RetentionPolicy.RUNTIME)
//@Target({FIELD, LOCAL_VARIABLE, METHOD, PARAMETER, TYPE})
public @interface PolyTainted {

}
