package checkers.interning.quals;

import static java.lang.annotation.ElementType.*;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * A polymorphic qualifier for the Interning type system.
 *
 * <p>
 * Any method written using @PolyInterned conceptually has two versions:  one
 * in which every instance of @PolyInterned has been replaced by @Interned, and
 * one in which every instance of @PolyInterned has been erased.
 */
@Documented
@TypeQualifier
@PolymorphicQualifier
@Retention(RetentionPolicy.RUNTIME)
//@Target({FIELD, LOCAL_VARIABLE, METHOD, PARAMETER, TYPE})
public @interface PolyInterned {

}
