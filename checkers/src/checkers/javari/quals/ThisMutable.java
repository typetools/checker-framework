package checkers.javari.quals;

import java.lang.annotation.Target;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * An annotation used to represent a place holder immutability type, that is
 * equivalent to the ThisMutable type in the Javari typesystem.
 * <p>
 *
 * This annotation may not be written in source code; it is an
 * implementation detail of the checker.
 */
@TypeQualifier
@Target({}) // empty target prevents programmers from writing this in a program
@SubtypeOf(ReadOnly.class)
public @interface ThisMutable {}
