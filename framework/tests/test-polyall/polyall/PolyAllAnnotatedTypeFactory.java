package polyall;

import java.lang.annotation.Annotation;
import java.util.Set;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;

import polyall.quals.H1Bot;
import polyall.quals.H1Poly;
import polyall.quals.H1S1;
import polyall.quals.H1S2;
import polyall.quals.H1Top;
import polyall.quals.H2Bot;
import polyall.quals.H2Poly;
import polyall.quals.H2S1;
import polyall.quals.H2S2;
import polyall.quals.H2Top;

public class PolyAllAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    public PolyAllAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return getBundledTypeQualifiersWithPolyAll(
                H1Top.class, H1S1.class, H1S2.class, H1Bot.class,
                H2Top.class, H2S1.class, H2S2.class, H2Bot.class,
                H1Poly.class, H2Poly.class);
    }

    @Override
    protected MultiGraphFactory createQualifierHierarchyFactory() {
        return new MultiGraphQualifierHierarchy.MultiGraphFactory(this);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new MultiGraphQualifierHierarchy(factory);
    }
}