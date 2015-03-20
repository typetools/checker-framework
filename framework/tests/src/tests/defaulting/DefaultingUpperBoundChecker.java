package tests.defaulting;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.TypeQualifiers;

import tests.defaulting.UpperBoundQual.UB_BOTTOM;
import tests.defaulting.UpperBoundQual.UB_EXPLICIT;
import tests.defaulting.UpperBoundQual.UB_IMPLICIT;
import tests.defaulting.UpperBoundQual.UB_TOP;

@TypeQualifiers({UB_TOP.class, UB_EXPLICIT.class, UB_IMPLICIT.class, UB_BOTTOM.class})
public class DefaultingUpperBoundChecker extends BaseTypeChecker {
}
