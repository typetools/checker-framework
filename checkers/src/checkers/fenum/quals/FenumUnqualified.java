package checkers.fenum.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * An unqualified type.  Such a type is incomparable to (that is, neither a
 * subtype nor a supertype of) any fake enum type.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
// @Target( {} )
@TypeQualifier
@SubtypeOf( { FenumTop.class } )
public @interface FenumUnqualified {}
