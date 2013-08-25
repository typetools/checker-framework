package checkers.fenum.quals;

import checkers.quals.DefaultFor;
import checkers.quals.DefaultLocation;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The top of the fake enumeration type hierarchy.
 * <p>
 *
 * This annotation may not be written in source code; it is an
 * implementation detail of the checker.
 *
 * @checker.framework.manual #fenum-checker Fake Enum Checker
 */
@TypeQualifier
@SubtypeOf( { } )
@DefaultFor(DefaultLocation.LOCALS)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({}) // empty target prevents programmers from writing this in a program
public @interface FenumTop {}
