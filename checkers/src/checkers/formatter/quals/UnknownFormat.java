package checkers.formatter.quals;

import java.lang.annotation.Target;

import checkers.quals.DefaultQualifierInHierarchy;
import checkers.quals.InvisibleQualifier;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * The top qualifier.
 *
 * <p>
 *
 * This annotation may not be written in source code; it is an
 * implementation detail of the checker.
 *
 * @checker_framework_manual #formatter-checker Format String Checker
 */
@TypeQualifier
@InvisibleQualifier
@SubtypeOf({})
@DefaultQualifierInHierarchy
@Target({})
public @interface UnknownFormat {}
