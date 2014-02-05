package checkers.regex.quals;

import java.lang.annotation.Target;

import checkers.quals.DefaultQualifierInHierarchy;
import checkers.quals.InvisibleQualifier;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * Represents the top of the Regex qualifier hierarchy.
 *
 * <p>
 *
 * This annotation may not be written in source code; it is an
 * implementation detail of the checker.
 *
 * @checker_framework_manual #regex-checker Regex Checker
 */
@TypeQualifier
@InvisibleQualifier
@DefaultQualifierInHierarchy
@SubtypeOf({})
@Target({}) // empty target prevents programmers from writing this in a program
public @interface UnknownRegex {}
