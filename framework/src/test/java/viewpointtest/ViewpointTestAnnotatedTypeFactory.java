package viewpointtest;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AbstractViewpointAdapter;

import viewpointtest.quals.A;
import viewpointtest.quals.B;
import viewpointtest.quals.Bottom;
import viewpointtest.quals.PolyVP;
import viewpointtest.quals.ReceiverDependentQual;
import viewpointtest.quals.Top;

import java.lang.annotation.Annotation;
import java.util.Set;

public class ViewpointTestAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    public ViewpointTestAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return getBundledTypeQualifiers(
                A.class,
                B.class,
                Bottom.class,
                PolyVP.class,
                ReceiverDependentQual.class,
                Top.class);
    }

    @Override
    protected AbstractViewpointAdapter createViewpointAdapter() {
        return new ViewpointTestViewpointAdapter(this);
    }
}
