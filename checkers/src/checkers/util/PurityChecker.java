package checkers.util;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;

/**
 * Perform purity checking only.
 */
@TypeQualifiers(Unqualified.class)
public class PurityChecker extends BaseTypeChecker {
}
