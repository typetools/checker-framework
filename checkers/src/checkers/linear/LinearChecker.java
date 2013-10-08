package checkers.linear;

import checkers.basetype.BaseTypeChecker;
import checkers.linear.quals.Linear;
import checkers.linear.quals.Normal;
import checkers.linear.quals.Unusable;
import checkers.quals.TypeQualifiers;

/**
 * A type-checker plug-in for the Linear type system.  A {@code @Linear}
 * reference may be used only one time.  After that, it is "used up" and
 * of type {@code @Unusable}, and any further use is a compile-time error.
 *
 * @checker.framework.manual #linear-checker Linear Checker

 */
@TypeQualifiers({Normal.class, Linear.class, Unusable.class})
public class LinearChecker extends BaseTypeChecker { }
