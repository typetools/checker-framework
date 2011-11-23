package checkers.quals;

import java.lang.annotation.Target;

/**
 * A special annotation intended solely for representing the bottom type in
 * the qualifier hierarchy.
 * This qualifier is only used if the existing qualifiers do not have a
 * bottom type.
 * 
 * @see checkers.types.QualifierHierarchy#getBottomAnnotations()
 *
 * <p>
 * Programmers cannot write this in source code.
 */
@TypeQualifier
@SubtypeOf({})
@Target({}) // empty target prevents programmers from writing this in a program
public @interface Bottom { }
