package org.checkerframework.framework.testchecker.nontopdefault;

import java.lang.annotation.Annotation;
import java.util.Set;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;

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
