package testlib.defaulting;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import testlib.defaulting.LowerBoundQual.LB_BOTTOM;
import testlib.defaulting.LowerBoundQual.LB_EXPLICIT;
import testlib.defaulting.LowerBoundQual.LB_IMPLICIT;
import testlib.defaulting.LowerBoundQual.LB_TOP;

public class DefaultingLowerBoundAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    public DefaultingLowerBoundAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new HashSet<Class<? extends Annotation>>(
                Arrays.asList(LB_TOP.class, LB_EXPLICIT.class, LB_IMPLICIT.class, LB_BOTTOM.class));
    }
}
