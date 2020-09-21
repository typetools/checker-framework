package org.checkerframework.framework.testchecker.reflection;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;

/** Checker for a simple type system to test reflection resolution. */
public class ReflectionTestChecker extends BaseTypeChecker {

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new ReflectionTestVisitor(this);
    }
}
