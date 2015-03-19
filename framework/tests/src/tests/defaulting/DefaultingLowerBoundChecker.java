package tests.defaulting;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.TypeQualifiers;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.util.PluginUtil;
import tests.defaulting.LowerBoundQual.*;

@TypeQualifiers({LB_TOP.class, LB_EXPLICIT.class, LB_IMPLICIT.class, LB_BOTTOM.class})
public class DefaultingLowerBoundChecker extends BaseTypeChecker {
}
