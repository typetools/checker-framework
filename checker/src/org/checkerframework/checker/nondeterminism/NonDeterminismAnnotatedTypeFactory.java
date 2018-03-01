package org.checkerframework.checker.nondeterminism;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.util.QualifierPolymorphism;

public class NonDeterminismAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    public NonDeterminismAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        postInit();
    }

    @Override
    protected QualifierPolymorphism createQualifierPolymorphism() {
        return new NonDetQualifierPolymorphism(processingEnv, this);
    }
}
