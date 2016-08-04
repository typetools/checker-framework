package tests.defaulting;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import tests.defaulting.UpperBoundQual.UB_BOTTOM;
import tests.defaulting.UpperBoundQual.UB_EXPLICIT;
import tests.defaulting.UpperBoundQual.UB_IMPLICIT;
import tests.defaulting.UpperBoundQual.UB_TOP;

public class DefaultingUpperBoundAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    public DefaultingUpperBoundAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new HashSet<Class<? extends Annotation>>(
                Arrays.asList(UB_TOP.class, UB_EXPLICIT.class, UB_IMPLICIT.class, UB_BOTTOM.class));
    }
}
