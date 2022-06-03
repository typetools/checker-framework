package org.checkerframework.framework.testchecker.typedeclbounds;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.testchecker.typedeclbounds.quals.Bottom;
import org.checkerframework.framework.testchecker.typedeclbounds.quals.S1;
import org.checkerframework.framework.testchecker.typedeclbounds.quals.S2;
import org.checkerframework.framework.testchecker.typedeclbounds.quals.Top;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TypeDeclBoundsAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    public TypeDeclBoundsAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new HashSet<>(Arrays.asList(Top.class, Bottom.class, S1.class, S2.class));
    }
}
