package checkers.signature.quals;

import java.lang.annotation.Target;

import checkers.quals.DefaultQualifierInHierarchy;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * Top qualifier in the type hierarchy.
 * <p>
 *
 * This annotation may not be written in source code; it is an
 * implementation detail of the checker.
 */
@DefaultQualifierInHierarchy
@TypeQualifier
@SubtypeOf({})
@Target({}) // empty target prevents programmers from writing this in a program
public @interface UnannotatedString {}
