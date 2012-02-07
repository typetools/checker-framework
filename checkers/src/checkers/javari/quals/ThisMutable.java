package checkers.javari.quals;

import java.lang.annotation.Target;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * An annotation used to represent a place holder immutability type, that is
 * equivalent to the ThisMutable type on the Javari typesystem.
 *
 * However, it is an implementation detail; hence, the package-scope.
 */
@TypeQualifier
@Target({}) // A programmer cannot write @ThisMutable in a program
@SubtypeOf(ReadOnly.class)
public @interface ThisMutable {}
