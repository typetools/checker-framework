package org.checkerframework.framework.testchecker.lubglb;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.testchecker.lubglb.quals.A;
import org.checkerframework.framework.testchecker.lubglb.quals.B;
import org.checkerframework.framework.testchecker.lubglb.quals.C;
import org.checkerframework.framework.testchecker.lubglb.quals.D;
import org.checkerframework.framework.testchecker.lubglb.quals.E;
import org.checkerframework.framework.testchecker.lubglb.quals.F;
import org.checkerframework.framework.testchecker.lubglb.quals.Poly;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class LubGlbAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    public LubGlbAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new HashSet<Class<? extends Annotation>>(
                Arrays.asList(A.class, B.class, C.class, D.class, E.class, F.class, Poly.class));
    }
}
