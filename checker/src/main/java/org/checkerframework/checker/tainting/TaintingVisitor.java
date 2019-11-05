package org.checkerframework.checker.tainting;

import javax.lang.model.element.ExecutableElement;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;

public class TaintingVisitor extends BaseTypeVisitor<TaintingAnnotatedTypeFactory> {

    public TaintingVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    protected void checkConstructorResult(
            AnnotatedExecutableType constructorType, ExecutableElement constructorElement) {
        // skip
    }
}
