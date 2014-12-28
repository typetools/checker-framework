package tests.defaulting;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.TypeQualifiers;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.util.PluginUtil;
import tests.defaulting.UpperBoundQual.*;


@TypeQualifiers({UB_TOP.class, UB_EXPLICIT.class, UB_IMPLICIT.class, UB_BOTTOM.class})
public class DefaultingUpperBoundChecker extends BaseTypeChecker {
}
