package org.checkerframework.checker.lowerbound;

import org.checkerframework.checker.lowerbound.qual.*;

import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.basetype.BaseTypeChecker;

import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ExpressionTree;

import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;

public class LowerBoundVisitor extends BaseTypeVisitor<LowerBoundAnnotatedTypeFactory> {

    private static final /*@CompilerMessageKey*/ String LOWER_BOUND = "array.access.unsafe.low";

    public LowerBoundVisitor(BaseTypeChecker checker) {
	super(checker);
    }

    @Override
    public Void visitArrayAccess(ArrayAccessTree tree, Void type) {
	ExpressionTree index = tree.getIndex();
	String arrName = tree.getExpression().toString();
	AnnotatedTypeMirror indexType = atypeFactory.getAnnotatedType(index);
	if (!(indexType.hasAnnotation(NonNegative.class) || indexType.hasAnnotation(Positive.class))) {
	    checker.report(Result.warning(LOWER_BOUND, indexType.toString(), arrName), index);
	}

	return super.visitArrayAccess(tree, type);
    }
}
