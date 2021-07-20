package org.checkerframework.framework.util;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.PurityUnqualified;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** AnnotatedTypeFactory for the {@link PurityChecker}. */
public class PurityAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    public PurityAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new HashSet<>(Arrays.asList(PurityUnqualified.class));
    }
}
