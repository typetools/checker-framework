package checkers.fenum.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * The top of the fake enumeration type hierarchy.
 * <p>
 *
 * This annotation may not be written in source code; it is an
 * implementation detail of the checker.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({}) // empty target prevents programmers from writing this in a program
@TypeQualifier
@SubtypeOf( { } )
public @interface FenumTop {}
