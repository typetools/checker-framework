package checkers.tainting.quals;

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
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface PolyTainted {}
