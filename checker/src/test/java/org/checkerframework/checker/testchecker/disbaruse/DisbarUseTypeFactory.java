package org.checkerframework.checker.testchecker.disbaruse;

import org.checkerframework.checker.testchecker.disbaruse.qual.DisbarUseBottom;
import org.checkerframework.checker.testchecker.disbaruse.qual.DisbarUseTop;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class DisbarUseTypeFactory extends BaseAnnotatedTypeFactory {
    public DisbarUseTypeFactory(BaseTypeChecker checker) {
        super(checker);
        postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new LinkedHashSet<>(Arrays.asList(DisbarUseTop.class, DisbarUseBottom.class));
    }
}
