package org.checkerframework.checker.upperbound;

import org.checkerframework.checker.upperbound.qual.*;

import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.basetype.BaseTypeChecker;

import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ExpressionTree;

import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;

public class UpperBoundVisitor extends BaseTypeVisitor<UpperBoundAnnotatedTypeFactory> {

    private static final /*@CompilerMessageKey*/ String UPPER_BOUND = "array.access.unsafe.high";

    public UpperBoundVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    public Void visitArrayAccess(ArrayAccessTree tree, Void type) {
        ExpressionTree index = tree.getIndex();
        String arrName = tree.getExpression().toString();
        AnnotatedTypeMirror indexType = atypeFactory.getAnnotatedType(index);
        if (!indexType.hasAnnotation(LessThanLength.class) ||
            !(UpperBoundUtils.hasValue(indexType, arrName))) {
            checker.report(Result.warning(UPPER_BOUND, indexType.toString(), arrName), index);
        }

        return super.visitArrayAccess(tree, type);
    }
}
