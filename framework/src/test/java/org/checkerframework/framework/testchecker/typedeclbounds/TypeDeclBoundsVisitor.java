package org.checkerframework.framework.testchecker.typedeclbounds;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;

public class TypeDeclBoundsVisitor extends BaseTypeVisitor<TypeDeclBoundsAnnotatedTypeFactory> {
    public TypeDeclBoundsVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    protected TypeDeclBoundsAnnotatedTypeFactory createTypeFactory() {
        return new TypeDeclBoundsAnnotatedTypeFactory(checker);
    }
}
