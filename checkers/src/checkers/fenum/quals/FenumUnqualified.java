package checkers.fenum.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * An unqualified type.  Such a type is incomparable to (that is, neither a
 * subtype nor a supertype of) any fake enum type.
 * <p>
 *
 * This annotation may not be written in source code; it is an
 * implementation detail of the checker.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({}) // empty target prevents programmers from writing this in a program
@TypeQualifier
@SubtypeOf( { FenumTop.class } )
public @interface FenumUnqualified {}
