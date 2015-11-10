package tests.defaulting;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.TypeQualifiers;

/**
 * Created by smillst on 11/3/15.
 */
@TypeQualifiers({FieldQual.F_FIELD.class, FieldQual.F_BOTTOM.class, FieldQual.F_TOP.class, FieldQual.F_MEMBER_SELECT.class})
public class DefaultingFieldChecker extends BaseTypeChecker {
}
