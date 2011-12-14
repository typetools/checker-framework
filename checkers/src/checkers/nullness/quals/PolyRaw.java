package checkers.nullness.quals;

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
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface PolyRaw {}
