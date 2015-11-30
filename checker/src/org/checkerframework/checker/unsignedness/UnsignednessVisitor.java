package org.checkerframework.checker.unsignedness;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree.Kind;

import org.checkerframework.checker.unsignedness.qual.*;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

public class UnsignednessVisitor extends BaseTypeVisitor<UnsignednessTypeFactory> {

	public UnsignednessVisitor(BaseTypeChecker checker) {
		super(checker);
	}

	protected UnsignednessVisitor(BaseTypeChecker checker, 
				UnsignednessTypeFactory factory) {

		super(checker, factory);
	}

	@Override
	protected UnsignednessTypeFactory createTypeFactory() {
		return new UnsignednessTypeFactory(checker);
	}

	@Override
	public Void visitBinary(BinaryTree node, Void p) {

		super.visitBinary(node, p);

		ExpressionTree leftOp = node.getLeftOperand();
		ExpressionTree rightOp = node.getRightOperand();
		AnnotatedTypeMirror leftOpType = atypeFactory.getAnnotatedType(leftOp);
		AnnotatedTypeMirror rightOpType = atypeFactory.getAnnotatedType(rightOp);

		Kind kind = node.getKind();

		switch(kind) {
			
			case DIVIDE:
			case GREATER_THAN:
			case GREATER_THAN_EQUAL:
			case LESS_THAN:
			case LESS_THAN_EQUAL:
			case REMAINDER:

				if(leftOpType.hasAnnotation(Unsigned.class)) {
					
					checker.report(Result.failure("binary.operation.type.incompatible",
						kind, leftOpType), node);
				}

				else if(rightOpType.hasAnnotation(Unsigned.class)) {
					
					checker.report(Result.failure("binary.operation.type.incompatible",
						kind, rightOpType), node);
				}
				break;

			case RIGHT_SHIFT:

				if(leftOpType.hasAnnotation(Unsigned.class)) {
					
					checker.report(Result.failure("binary.operation.type.incompatible",
						kind, leftOpType), node);
				}
				break;

			case UNSIGNED_RIGHT_SHIFT:

				if(leftOpType.hasAnnotation(Signed.class)) {

					checker.report(Result.failure("binary.operation.type.incompatible",
						kind, leftOpType), node);
				}
				break;

			default:

				if((leftOpType.hasAnnotation(Unsigned.class) && 
						rightOpType.hasAnnotation(Signed.class)) || 
					(leftOpType.hasAnnotation(Signed.class) &&
						rightOpType.hasAnnotation(Unsigned.class))){

					checker.report(Result.failure("binary.comparison.type.incompatible",
						kind), node);
				}
				break;

		}

		return null;
	}

	@Override
	public Void visitCompoundAssignment(CompoundAssignmentTree node, Void p) {
	
		super.visitCompoundAssignment(node, p);

		ExpressionTree var = node.getVariable();
		ExpressionTree expr = node.getExpression();
		AnnotatedTypeMirror varType = atypeFactory.getAnnotatedType(var);
		AnnotatedTypeMirror exprType = atypeFactory.getAnnotatedType(expr);

		Kind kind = node.getKind();

		switch(kind) {
			
			case DIVIDE_ASSIGNMENT:
			case REMAINDER_ASSIGNMENT:

				if(varType.hasAnnotation(Unsigned.class)) {
					
					checker.report(Result.failure("binary.operation.type.incompatible",
						kind, varType), node);
				}

				else if(exprType.hasAnnotation(Unsigned.class)) {
					
					checker.report(Result.failure("binary.operation.type.incompatible",
						kind, exprType), node);
				}
				break;

			case RIGHT_SHIFT_ASSIGNMENT:

				if(varType.hasAnnotation(Unsigned.class)) {
					
					checker.report(Result.failure("binary.operation.type.incompatible",
						kind, varType), node);
				}
				break;

			case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT:

				if(varType.hasAnnotation(Signed.class)) {

					checker.report(Result.failure("binary.operation.type.incompatible",
						kind, varType), node);
				}
				break;

			default:
				if((varType.hasAnnotation(Unsigned.class) && 
						exprType.hasAnnotation(Signed.class)) || 
					(varType.hasAnnotation(Signed.class) && 
						exprType.hasAnnotation(Unsigned.class))) {

					checker.report(Result.failure("binary.comparison.type.incompatible", 
						kind), node);
				}
				break;

		}

		return null;
	}
}