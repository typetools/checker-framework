package org.checkerframework.checker.lowerbound;

import org.checkerframework.checker.lowerbound.qual.*;

import javax.lang.model.element.AnnotationMirror;

import org.checkerframework.javacutil.AnnotationUtils;

import org.checkerframework.framework.type.treeannotator.TreeAnnotator;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

import com.sun.source.tree.Tree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.BinaryTree;

public class LowerBoundAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
    private final AnnotationMirror N1P, NN, POS, UNKNOWN;

    public LowerBoundAnnotatedTypeFactory(BaseTypeChecker checker) {
	super(checker);
	N1P = AnnotationUtils.fromClass(elements, NegativeOnePlus.class);
	NN = AnnotationUtils.fromClass(elements, NonNegative.class);
	POS = AnnotationUtils.fromClass(elements, Positive.class);
	UNKNOWN = AnnotationUtils.fromClass(elements, Unknown.class);

	this.postInit();
    }

    // this is apparently just a required thing
    @Override
    public TreeAnnotator createTreeAnnotator() {
	return new LowerBoundTreeAnnotator(this);
    }

    private class LowerBoundTreeAnnotator extends TreeAnnotator{
	public LowerBoundTreeAnnotator(AnnotatedTypeFactory annotatedTypeFactory) {
	    super(annotatedTypeFactory);
	}

	// annotate literal integers appropriately
	@Override
	public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
            // if this is an Integer specifically
            if (tree.getKind() == Tree.Kind.INT_LITERAL) {
                int val = (int) tree.getValue();
		if (val == -1) {
		    type.addAnnotation(N1P);
		} else if (val == 0) {
                    type.addAnnotation(NN);
                } else if (val > 0) {
		    type.addAnnotation(POS);
		}
            } // no else, only annotate Integers
            return super.visitLiteral(tree, type);
        }

	@Override
	public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
            ExpressionTree left = tree.getLeftOperand();
            ExpressionTree right = tree.getRightOperand();
            switch (tree.getKind()) {
	    case PLUS:
                plusHelper(left, right, type);
                break;
	    case MINUS:
                minusHelper(left, right, type);
                break;
	    case MULTIPLY:
		timesHelper(left, right, type);
		break;
            default:
                break;
            }
	    return super.visitBinary(tree, type);
        }

	public void plusHelper(ExpressionTree leftExpr, ExpressionTree rightExpr,
			       AnnotatedTypeMirror type) {
	    AnnotatedTypeMirror leftType = getAnnotatedType(leftExpr);
            AnnotatedTypeMirror rightType = getAnnotatedType(rightExpr);
            // if left is literal 1/0/2 and right is not a literal swap them because we already handle the transfer for that
            // and it would be redundant to repeat it all again
            // we don't want right to be a literal too b/c we could be swapping forever
            if (leftExpr.getKind() == Tree.Kind.INT_LITERAL &&
		!(rightExpr.getKind() == Tree.Kind.INT_LITERAL)) {
                int val = (int)((LiteralTree)leftExpr).getValue();
                if (val == 1 || val == 0 || val == 2) {
                    plusHelper(rightExpr, leftExpr, type);
                    return;
                }
            }
            // if the right side is a literal we do some special stuff(specifically for 1 and 0)
            if (rightExpr.getKind() == Tree.Kind.INT_LITERAL) {
                int val = (int)((LiteralTree)rightExpr).getValue();
                if (val == 1) {
		    if (leftType.hasAnnotation(N1P)) {
			type.addAnnotation(NN);
		    } else if (leftType.hasAnnotation(NN)) {
			type.addAnnotation(POS);
		    } else if (leftType.hasAnnotation(POS)) {
			type.addAnnotation(POS);
		    }
                    return;
                }
                // if we are adding 0 dont change type
                else if (val == 0) {
		    type.addAnnotation(leftType.getAnnotationInHierarchy(POS));
                    return;
                }
		else if (val == 2) {
		    if (leftType.hasAnnotation(N1P)) {
			type.addAnnotation(POS);
			return;
		    }
		}
	    }
	    // pos + pos -> pos
	    // pos + nn -> pos
	    // nn + nn -> nn
	    // pos + n1p -> nn
	    // nn + n1p -> n1p
	    if (leftType.hasAnnotation(POS) && rightType.hasAnnotation(POS)) {
		type.addAnnotation(POS);
		return;
	    }
	    if ((leftType.hasAnnotation(POS) && rightType.hasAnnotation(NN)) ||
		(leftType.hasAnnotation(NN) && rightType.hasAnnotation(POS))) {
		type.addAnnotation(POS);
		return;
	    }
	    if (leftType.hasAnnotation(NN) && rightType.hasAnnotation(NN)) {
		type.addAnnotation(NN);
		return;
	    }
	    if ((leftType.hasAnnotation(POS) && rightType.hasAnnotation(N1P)) ||
		(leftType.hasAnnotation(N1P) && rightType.hasAnnotation(POS))) {
		type.addAnnotation(NN);
		return;
	    }
	    if ((leftType.hasAnnotation(N1P) && rightType.hasAnnotation(NN)) ||
		(leftType.hasAnnotation(NN) && rightType.hasAnnotation(N1P))) {
		type.addAnnotation(N1P);
		return;
	    }

	}
	public void minusHelper(ExpressionTree leftExpr, ExpressionTree rightExpr,
			       AnnotatedTypeMirror type) {
	    AnnotatedTypeMirror leftType = getAnnotatedType(leftExpr);
            AnnotatedTypeMirror rightType = getAnnotatedType(rightExpr);
            // if the right side is a literal we do some special stuff(specifically for 1 and 0)
            if (rightExpr.getKind() == Tree.Kind.INT_LITERAL) {
                int val = (int)((LiteralTree)rightExpr).getValue();
                if (val == 1) {
		    if (leftType.hasAnnotation(N1P)) {
			type.addAnnotation(UNKNOWN);
		    } else if (leftType.hasAnnotation(NN)) {
			type.addAnnotation(N1P);
		    } else if (leftType.hasAnnotation(POS)) {
			type.addAnnotation(NN);
		    }
                    return;
                }
                // if we are adding 0 dont change type
                else if (val == 0) {
		    type.addAnnotation(leftType.getAnnotationInHierarchy(POS));
                    return;
                }
		else if (val == 2) {
		    if (leftType.hasAnnotation(POS)) {
			type.addAnnotation(N1P);
		    }
		    return;
		}
	    }
	}
	public void timesHelper(ExpressionTree leftExpr, ExpressionTree rightExpr,
			       AnnotatedTypeMirror type) {
	    AnnotatedTypeMirror leftType = getAnnotatedType(leftExpr);
            AnnotatedTypeMirror rightType = getAnnotatedType(rightExpr);

            // if left is literal 1/0 and right is not a literal swap them because we already handle the transfer for that
            // and it would be redundant to repeat it all again
            // we don't want right to be a literal too b/c we could be swapping forever
            if (leftExpr.getKind() == Tree.Kind.INT_LITERAL &&
		!(rightExpr.getKind() == Tree.Kind.INT_LITERAL)) {
                int val = (int)((LiteralTree)leftExpr).getValue();
                if (val == 1 || val == 0) {
                    plusHelper(rightExpr, leftExpr, type);
                    return;
                }
            }

	    // if the right side is a literal we do some special stuff(specifically for 1 and 0)
            if (rightExpr.getKind() == Tree.Kind.INT_LITERAL) {
                int val = (int)((LiteralTree)rightExpr).getValue();
                if (val == 1) {
		    type.addAnnotation(leftType.getAnnotationInHierarchy(POS));
                    return;
                }
		else if (val == 0) {
		    type.addAnnotation(NN);
                    return;
                }
	    }

	    // deal with two things for which we have reasonable annotations
	    // pos * pos -> pos
	    // nn * pos -> nn
	    // nn * nn -> nn
	    if (leftType.hasAnnotation(POS) && rightType.hasAnnotation(POS)) {
		type.addAnnotation(POS);
		return;
	    }
	    if ((leftType.hasAnnotation(POS) && rightType.hasAnnotation(NN)) ||
		(leftType.hasAnnotation(NN) && rightType.hasAnnotation(POS))) {
		type.addAnnotation(NN);
		return;
	    }
	    if (leftType.hasAnnotation(NN) && rightType.hasAnnotation(NN)) {
		type.addAnnotation(NN);
		return;
	    }
	}
    }
}
