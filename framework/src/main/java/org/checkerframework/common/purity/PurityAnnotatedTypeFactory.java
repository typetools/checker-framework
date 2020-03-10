package org.checkerframework.common.purity;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.purity.qual.PurityUnqualified;

/** The Purity Checker is used to determine if methods are deterministic and side-effect-free. */
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
