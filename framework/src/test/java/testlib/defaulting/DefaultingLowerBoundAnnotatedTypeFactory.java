package testlib.defaulting;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import testlib.defaulting.LowerBoundQual.LbBottom;
import testlib.defaulting.LowerBoundQual.LbExplicit;
import testlib.defaulting.LowerBoundQual.LbImplicit;
import testlib.defaulting.LowerBoundQual.LbTop;

public class DefaultingLowerBoundAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    public DefaultingLowerBoundAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new HashSet<Class<? extends Annotation>>(
                Arrays.asList(LbTop.class, LbExplicit.class, LbImplicit.class, LbBottom.class));
    }
}
