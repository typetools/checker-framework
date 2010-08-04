package checkers.fenum.quals;

import java.lang.annotation.*;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * The top of the type hierarchy.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
// TODO: should we allow it in some places?
// @Target( {} )
@TypeQualifier
@SubtypeOf( { } )
public @interface FenumTop {}
