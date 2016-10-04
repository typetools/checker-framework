package org.checkerframework.checker.minlen;

import com.sun.source.tree.TypeCastTree;
import javax.lang.model.type.TypeKind;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.javacutil.InternalUtils;

public class MinLenVisitor extends BaseTypeVisitor<MinLenAnnotatedTypeFactory> {
    public MinLenVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    public Void visitTypeCast(TypeCastTree node, Void type) {
        if (InternalUtils.typeOf(node.getExpression()).getKind() != TypeKind.ARRAY
                && InternalUtils.typeOf(node.getType()).getKind() == TypeKind.ARRAY) {
            return type;
        } else {
            return super.visitTypeCast(node, type);
        }
    }
}
