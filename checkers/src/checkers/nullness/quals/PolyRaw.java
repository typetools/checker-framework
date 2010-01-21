package checkers.nullness.quals;

import static java.lang.annotation.ElementType.*;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * A polymorphic qualifier for the Rawness type system.
 *
 * <p>
 * Any method written using @PolyRaw conceptually has two versions:  one
 * in which every instance of @PolyRaw has been replaced by @Raw, and
 * one in which every instance of @PolyRaw has been replaced by @NonRaw.
 */
@Documented
@TypeQualifier
@PolymorphicQualifier
@Retention(RetentionPolicy.RUNTIME)
//@Target({FIELD, LOCAL_VARIABLE, METHOD, PARAMETER, TYPE})
public @interface PolyRaw {

}
