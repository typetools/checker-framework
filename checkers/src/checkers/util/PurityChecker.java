package checkers.util;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;
import checkers.types.BasicAnnotatedTypeFactory;

/**
 * Perform purity checking only.
 */
@TypeQualifiers(Unqualified.class)
public class PurityChecker extends BaseTypeChecker<BasicAnnotatedTypeFactory<PurityChecker>> {
}
