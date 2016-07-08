package org.checkerframework.checker.lowerbound;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;

import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;

import org.checkerframework.checker.lowerbound.qual.*;

import org.checkerframework.common.basetype.BaseTypeChecker;

import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;

import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.Pair;

public class LowerBoundAnnotatedTypeFactory extends
 GenericAnnotatedTypeFactory<CFValue, CFStore, LowerBoundTransfer, LowerBoundAnalysis> {

    private final AnnotationMirror N1P, NN, POS, UNKNOWN;

    public LowerBoundAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        N1P = AnnotationUtils.fromClass(elements, NegativeOnePlus.class);
        NN = AnnotationUtils.fromClass(elements, NonNegative.class);
        POS = AnnotationUtils.fromClass(elements, Positive.class);
        UNKNOWN = AnnotationUtils.fromClass(elements, LowerBoundUnknown.class);
        this.postInit();
    }

    /** These getter methods exist so that LowerBoundTransfer can use these objects */
    public AnnotationMirror getN1P() {
        return N1P;
    }

    public AnnotationMirror getNN() {
        return NN;
    }

    public AnnotationMirror getPOS() {
        return POS;
    }

    public AnnotationMirror getUNKNOWN() {
        return UNKNOWN;
    }

    @Override
    protected LowerBoundAnalysis createFlowAnalysis(
            List<Pair<VariableElement, CFValue>> fieldValues) {
        return new LowerBoundAnalysis(checker, this, fieldValues);
    }

    /** this is apparently just a required thing */
    @Override
    public TreeAnnotator createTreeAnnotator() {
        return new LowerBoundTreeAnnotator(this);
    }

    private class LowerBoundTreeAnnotator extends TreeAnnotator{
        public LowerBoundTreeAnnotator(AnnotatedTypeFactory annotatedTypeFactory) {
            super(annotatedTypeFactory);
        }

        /** does the actual work of annotating a literal so that we can call this from
            elsewhere in this program (e.g. plusHelper w/ two literals) */
        private void literalHelper(int val, AnnotatedTypeMirror type) {
            if (val == -1) {
                type.addAnnotation(N1P);
            } else if (val == 0) {
                type.addAnnotation(NN);
            } else if (val > 0) {
                type.addAnnotation(POS);
            } else {
                type.addAnnotation(UNKNOWN);
            }
        }

        /**
         * annotate literal integers appropriately
         */
        @Override
        public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
            /** only annotate integers */
            if (tree.getKind() == Tree.Kind.INT_LITERAL) {
                int val = (int) tree.getValue();
                literalHelper(val, type);
            }
            return super.visitLiteral(tree, type);
        }

        /** call increment and decrement helper functions */
        @Override
        public Void visitUnary(UnaryTree tree, AnnotatedTypeMirror type) {
            AnnotatedTypeMirror leftType = getAnnotatedType(tree.getExpression());
            switch (tree.getKind()) {
            case PREFIX_INCREMENT:
            case POSTFIX_INCREMENT:
                incrementHelper(leftType, type);
                break;
            case PREFIX_DECREMENT:
            case POSTFIX_DECREMENT:
                decrementHelper(leftType, type);
                break;
            default:
                break;
            }
            return super.visitUnary(tree, type);
        }

        /** handles x+1, x++, ++x, and all equivalent statements */
        public void incrementHelper(AnnotatedTypeMirror leftType, AnnotatedTypeMirror type) {
            if (leftType.hasAnnotation(N1P)) {
                type.addAnnotation(NN);
            } else if (leftType.hasAnnotation(NN)) {
                type.addAnnotation(POS);
            } else if (leftType.hasAnnotation(POS)) {
                type.addAnnotation(POS);
            } else {
                type.addAnnotation(UNKNOWN);
            }
            return;
        }

        /** handles x-1, x--, --x, and all equivalent statements */
        public void decrementHelper(AnnotatedTypeMirror leftType, AnnotatedTypeMirror type) {
            if (leftType.hasAnnotation(NN)) {
                type.addAnnotation(N1P);
            } else if (leftType.hasAnnotation(POS)) {
                type.addAnnotation(NN);
            } else {
                type.addAnnotation(UNKNOWN);
            }
            return;
        }

        /** dispatch to binary operator helper methods. the lower bound checker currently
         *  handles addition, subtraction, multiplication, division, and modular division */
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
            case DIVIDE:
                divideHelper(left, right, type);
                break;
            case REMAINDER:
                modHelper(left, right, type);
                break;
            default:
                break;
            }
            return super.visitBinary(tree, type);
        }

        /**   plusHelper handles the following cases:
               int lit + int lit -> do the math
               lit 0 + * -> *
               lit 1 + * -> call increment
               lit -1 + * -> call decrement
               lit >= 2 + n1p, nn, or pos -> pos
               lit -2 + pos -> n1p
               let all other lits fall through:
               pos + pos -> pos
               pos + nn -> pos
               nn + nn -> nn
               pos + n1p -> nn
               nn + n1p -> n1p
               * + * -> lbu
         */

        public void plusHelper(ExpressionTree leftExpr, ExpressionTree rightExpr,
                               AnnotatedTypeMirror type) {
            AnnotatedTypeMirror leftType = getAnnotatedType(leftExpr);
            AnnotatedTypeMirror rightType = getAnnotatedType(rightExpr);

            /** if both left and right are literals, do the math...*/
            if (leftExpr.getKind() == Tree.Kind.INT_LITERAL &&
               rightExpr.getKind() == Tree.Kind.INT_LITERAL) {
                int valLeft = (int)((LiteralTree)leftExpr).getValue();
                int valRight = (int)((LiteralTree)rightExpr).getValue();
                int valResult = valLeft + valRight;
                literalHelper(valResult, type);
                return;
            }

            /** if the left side is a literal, commute it to the right
                and rerun to avoid duplicating code.
                We can do this because we already checked if both are literals.
            */
            if (leftExpr.getKind() == Tree.Kind.INT_LITERAL) {
                int val = (int)((LiteralTree)leftExpr).getValue();
                if (val >= -1) {
                    plusHelper(rightExpr, leftExpr, type);
                    return;
                }
            }

            /** handle the case where one of the two is an interesting literal. */
            if (rightExpr.getKind() == Tree.Kind.INT_LITERAL) {
                int val = (int)((LiteralTree)rightExpr).getValue();
                if (val == -2) {
                    if (leftType.hasAnnotation(POS)) {
                        type.addAnnotation(N1P);
                        return;
                    }
                }
                else if (val == -1) {
                    decrementHelper(leftType, type);
                    return;
                }
                else if (val == 0) {
                    type.addAnnotation(leftType.getAnnotationInHierarchy(POS));
                    return;
                }
                else if (val == 1) {
                    incrementHelper(leftType, type);
                    return;
                }
                else if (val >= 2) {
                    if (leftType.hasAnnotation(N1P) || leftType.hasAnnotation(NN) ||
                        leftType.hasAnnotation(POS)) {
                        type.addAnnotation(POS);
                        return;
                    }
                }
            }
            /** This section is handling the generic cases:
             pos + pos -> pos
             pos + nn -> pos
             nn + nn -> nn
             pos + n1p -> nn
             nn + n1p -> n1p
            */
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
            /** * + * -> lbu */
            type.addAnnotation(UNKNOWN);
            return;
        }

        /** minusHelper handles the following cases:
               int lit - int lit -> do the math
               * - lit 0 -> *
               * - lit 1 -> call decrement
               * - lit -1 -> call increment
               pos - lit 2 -> n1p
               n1p, nn, pos - lit <= -2 -> pos
               * - * -> lbu
         */
        public void minusHelper(ExpressionTree leftExpr, ExpressionTree rightExpr,
                               AnnotatedTypeMirror type) {
            AnnotatedTypeMirror leftType = getAnnotatedType(leftExpr);
            AnnotatedTypeMirror rightType = getAnnotatedType(rightExpr);

            /** if both left and right are literals, do the math...*/
            if (leftExpr.getKind() == Tree.Kind.INT_LITERAL &&
               rightExpr.getKind() == Tree.Kind.INT_LITERAL) {
                int valLeft = (int)((LiteralTree)leftExpr).getValue();
                int valRight = (int)((LiteralTree)rightExpr).getValue();
                int valResult = valLeft - valRight;
                literalHelper(valResult, type);
                return;
            }

            /** special handling for literals on the right */
            if (rightExpr.getKind() == Tree.Kind.INT_LITERAL) {
                int val = (int)((LiteralTree)rightExpr).getValue();
                if (val == 2) {
                    if (leftType.hasAnnotation(POS)) {
                        type.addAnnotation(N1P);
                        return;
                    }
                }
                else if (val == 1) {
                    decrementHelper(leftType, type);
                    return;
                }
                else if (val == 0) {
                    type.addAnnotation(leftType.getAnnotationInHierarchy(POS));
                    return;
                }
                else if (val == -1) {
                    incrementHelper(leftType, type);
                }
                else if (val <= -2) {
                    if (leftType.hasAnnotation(N1P) || leftType.hasAnnotation(NN) ||
                        leftType.hasAnnotation(POS)) {
                        type.addAnnotation(POS);
                        return;
                    }
                }
            }

            /** we can't say anything about generic things that are being subtracted, sadly */
            type.addAnnotation(UNKNOWN);
            return;
        }

        /**
             timesHelper handles the following cases:
               int lit * int lit -> do the math
               * * lit 0 -> nn (=0)
               * * lit 1 -> *
               pos * pos -> pos
               pos * nn -> nn
               nn * nn -> nn
               * * * -> lbu
         */
        public void timesHelper(ExpressionTree leftExpr, ExpressionTree rightExpr,
                               AnnotatedTypeMirror type) {
            AnnotatedTypeMirror leftType = getAnnotatedType(leftExpr);
            AnnotatedTypeMirror rightType = getAnnotatedType(rightExpr);

            /** if both left and right are literals, do the math...*/
            if (leftExpr.getKind() == Tree.Kind.INT_LITERAL &&
               rightExpr.getKind() == Tree.Kind.INT_LITERAL) {
                int valLeft = (int)((LiteralTree)leftExpr).getValue();
                int valRight = (int)((LiteralTree)rightExpr).getValue();
                int valResult = valLeft * valRight;
                literalHelper(valResult, type);
                return;
            }

            /** because we already handle literals on the right, commute those on the left */
            if (leftExpr.getKind() == Tree.Kind.INT_LITERAL) {
                int val = (int)((LiteralTree)leftExpr).getValue();
                if (val == 0 || val == 1) {
                    timesHelper(rightExpr, leftExpr, type);
                    return;
                }
            }

            /** special handling for literals */
            if (rightExpr.getKind() == Tree.Kind.INT_LITERAL) {
                int val = (int)((LiteralTree)rightExpr).getValue();
                if (val == 0) {
                    type.addAnnotation(NN);
                    return;
                } else if (val == 1) {
                    type.addAnnotation(leftType.getAnnotationInHierarchy(POS));
                    return;
                }
            }

            /** this section handles generic annotations
                pos * pos -> pos
                nn * pos -> nn
                nn * nn -> nn
            */
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

        /**
               int lit / int lit -> do the math
               lit 0 / * -> nn
               * / lit 1 -> *
               pos / pos -> nn
               nn / pos -> nn
               pos / nn -> nn
               nn / nn -> nn
               pos / n1p -> n1p
               nn / n1p -> n1p
               n1p / n1p -> nn
               * / * -> lbu
         */
        public void divideHelper(ExpressionTree leftExpr, ExpressionTree rightExpr,
                               AnnotatedTypeMirror type) {
            AnnotatedTypeMirror leftType = getAnnotatedType(leftExpr);
            AnnotatedTypeMirror rightType = getAnnotatedType(rightExpr);

            /** if both left and right are literals, do the math...*/
            if (leftExpr.getKind() == Tree.Kind.INT_LITERAL &&
               rightExpr.getKind() == Tree.Kind.INT_LITERAL) {
                int valLeft = (int)((LiteralTree)leftExpr).getValue();
                int valRight = (int)((LiteralTree)rightExpr).getValue();
                int valResult = valLeft / valRight;
                literalHelper(valResult, type);
                return;
            }

            /** handle dividing zero by anything */
            if (leftExpr.getKind() == Tree.Kind.INT_LITERAL) {
                int val = (int)((LiteralTree)leftExpr).getValue();
                if (val == 0) {
                    type.addAnnotation(NN);
                    return;
                }
            }

            /** handle dividing by one. We assume that you aren't dividing by literal zero... */
            if (rightExpr.getKind() == Tree.Kind.INT_LITERAL) {
                int val = (int)((LiteralTree)rightExpr).getValue();
                if (val == 1) {
                    type.addAnnotation(leftType.getAnnotationInHierarchy(POS));
                    return;
                }
            }

            /** this section handles generic annotations
               pos / pos -> nn
               nn / pos -> nn
               pos / nn -> nn
               nn / nn -> nn
               n1p / pos -> n1p
               n1p / nn -> n1p
            */
            if (leftType.hasAnnotation(POS) && rightType.hasAnnotation(POS)) {
                type.addAnnotation(NN);
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
            if (leftType.hasAnnotation(N1P) && rightType.hasAnnotation(POS)) {
                type.addAnnotation(N1P);
                return;
            }
            if (leftType.hasAnnotation(N1P) && rightType.hasAnnotation(NN)) {
                type.addAnnotation(N1P);
                return;
            }
            /** we don't know anything about other stuff. */
            type.addAnnotation(UNKNOWN);
            return;
        }
        /**
           int lit % int lit -> do the math
           * % 1/-1 -> nn
           pos/nn % * -> nn
           n1p % * -> n1p
           * % * -> lbu
         */
        public void modHelper(ExpressionTree leftExpr, ExpressionTree rightExpr,
                               AnnotatedTypeMirror type) {
            AnnotatedTypeMirror leftType = getAnnotatedType(leftExpr);
            AnnotatedTypeMirror rightType = getAnnotatedType(rightExpr);

            /** if both left and right are literals, do the math...*/
            if (leftExpr.getKind() == Tree.Kind.INT_LITERAL &&
               rightExpr.getKind() == Tree.Kind.INT_LITERAL) {
                int valLeft = (int)((LiteralTree)leftExpr).getValue();
                int valRight = (int)((LiteralTree)rightExpr).getValue();
                int valResult = valLeft % valRight;
                literalHelper(valResult, type);
                return;
            }

            /** handle modding by one/negative one. */
            if (rightExpr.getKind() == Tree.Kind.INT_LITERAL) {
                int val = (int)((LiteralTree)rightExpr).getValue();
                if (val == 1 || val == -1) {
                    type.addAnnotation(NN);
                    return;
                }
            }

            /** this section handles generic annotations
                pos/nn % * -> nn
                n1p % * -> n1p
             */
            if (leftType.hasAnnotation(POS) || leftType.hasAnnotation(NN)) {
                type.addAnnotation(NN);
                return;
            }
            if (leftType.hasAnnotation(N1P)) {
                type.addAnnotation(N1P);
                return;
            }

            /** we don't know anything about other stuff. */
            type.addAnnotation(UNKNOWN);
            return;
        }
    }
}
