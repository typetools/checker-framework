package org.checkerframework.checkers.tainting;

import org.checkerframework.framework.base.DefaultQualifiedTypeFactory;
import org.checkerframework.framework.base.QualifierHierarchy;

public class TaintingQualifiedTypeFactory extends DefaultQualifiedTypeFactory<Tainting> {
    @Override
    protected QualifierHierarchy<Tainting> createQualifierHierarchy() {
        return new TaintingQualifierHierarchy();
    }

    @Override
    protected TaintingAnnotationConverter createAnnotationConverter() {
        return new TaintingAnnotationConverter();
    }
}
