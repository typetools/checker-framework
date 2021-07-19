package org.checkerframework.framework.testchecker.reflection;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;

/** Visitor for a simple type system to test reflection resolution. */
public final class ReflectionTestVisitor
        extends BaseTypeVisitor<ReflectionTestAnnotatedTypeFactory> {

    public ReflectionTestVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    protected ReflectionTestAnnotatedTypeFactory createTypeFactory() {
        return new ReflectionTestAnnotatedTypeFactory(checker);
    }
}
