package checkers.signature.quals;

import java.lang.annotation.Target;

import checkers.quals.DefaultQualifierInHierarchy;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * Top qualifier in the type hierarchy.
 * <p>
 * Not to be written by the programmer, only used internally.
 */
@DefaultQualifierInHierarchy
@TypeQualifier
@SubtypeOf({})
@Target({})
public @interface UnannotatedString {}
