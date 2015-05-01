package tests.defaulting;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.TypeQualifiers;

import tests.defaulting.LowerBoundQual.LB_BOTTOM;
import tests.defaulting.LowerBoundQual.LB_EXPLICIT;
import tests.defaulting.LowerBoundQual.LB_IMPLICIT;
import tests.defaulting.LowerBoundQual.LB_TOP;

@TypeQualifiers({LB_TOP.class, LB_EXPLICIT.class, LB_IMPLICIT.class, LB_BOTTOM.class})
public class DefaultingLowerBoundChecker extends BaseTypeChecker {
}
