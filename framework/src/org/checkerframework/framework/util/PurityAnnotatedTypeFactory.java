package org.checkerframework.framework.util;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PurityAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    public PurityAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return Collections.unmodifiableSet(
                new HashSet<Class<? extends Annotation>>(
                        Arrays.asList(PurityUnqualified.class)));
    }
}
