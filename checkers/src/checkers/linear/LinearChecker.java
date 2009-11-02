package checkers.linear;

import checkers.basetype.BaseTypeChecker;
import checkers.linear.quals.*;
import checkers.quals.TypeQualifiers;

/**
 * A typechecker plug-in for the Linear type system.  A {@code @Linear}
 * reference may be used only one time.  After that, it is "used up" and
 * of type {@code @Unusable}, and any further use is a compile-time error.
 */
@TypeQualifiers({Normal.class, Linear.class, Unusable.class})
public class LinearChecker extends BaseTypeChecker { }
