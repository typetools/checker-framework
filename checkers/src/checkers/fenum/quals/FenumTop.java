package checkers.fenum.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * The top of the fake enumeration type hierarchy.  This type should never
 * be written in source code; it is used internally by the fake enumeration
 * type system.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
// TODO: should we allow it in some places?
// @Target( {} )
@TypeQualifier
@SubtypeOf( { } )
public @interface FenumTop {}
