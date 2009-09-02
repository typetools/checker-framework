package checkers.nullness.quals;

import static java.lang.annotation.ElementType.*;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * A polymorphic qualifier for the Nullness type system.
 *
 * <p>
 * Any method written using @PolyNull conceptually has two versions:  one
 * in which every instance of @PolyNull has been replaced by @NonNull, and
 * one in which every instance of @PolyNull has been replaced by @Nullable.
 */
@Documented
@TypeQualifier
@PolymorphicQualifier
@Retention(RetentionPolicy.RUNTIME)
//@Target({FIELD, LOCAL_VARIABLE, METHOD, PARAMETER, TYPE})
public @interface PolyNull {

}
