package checkers.fenum.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * An unqualified type.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
// @Target( {} )
@TypeQualifier
@SubtypeOf( { FenumTop.class } )
public @interface FenumUnqualified {}
