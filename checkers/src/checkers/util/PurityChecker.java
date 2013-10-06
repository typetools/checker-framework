package checkers.util;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;
import checkers.types.SubtypingAnnotatedTypeFactory;

/**
 * Perform purity checking only.
 */
@TypeQualifiers(Unqualified.class)
public class PurityChecker extends BaseTypeChecker<SubtypingAnnotatedTypeFactory<PurityChecker>> {
}
