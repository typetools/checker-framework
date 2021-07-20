package org.checkerframework.framework.testchecker.nontopdefault;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;

import java.lang.annotation.Annotation;
import java.util.Set;

public class NTDAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    public NTDAnnotatedTypeFactory(BaseTypeChecker checker) {
        // use flow inference
        super(checker, true);
        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        // there's no polymorphic qualifiers in NTD
        return getBundledTypeQualifiers();
    }
}
