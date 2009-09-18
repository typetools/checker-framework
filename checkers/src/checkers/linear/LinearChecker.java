package checkers.linear;

import checkers.basetype.BaseTypeChecker;
import checkers.linear.quals.*;
import checkers.quals.TypeQualifiers;

//  * A typechecker plug-in for the Rawness type system qualifier that finds (and
//* verifies the absence of) null-pointer errors.

/**
 * A typechecker plug-in for the Linear type system qualifier that finds (and
 * verifies the absence of) usage
 */
@TypeQualifiers({Normal.class, Linear.class, Unusable.class})
public class LinearChecker extends BaseTypeChecker { }
