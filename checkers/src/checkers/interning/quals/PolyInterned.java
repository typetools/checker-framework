package checkers.interning.quals;

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
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface PolyInterned {}
