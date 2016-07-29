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

import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.qual.IntVal;

import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.treeannotator.ImplicitsTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;

import org.checkerframework.framework.type.treeannotator.TreeAnnotator;

import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.Pair;

public class LowerBoundAnnotatedTypeFactory extends
 GenericAnnotatedTypeFactory<CFValue, CFStore, LowerBoundTransfer, LowerBoundAnalysis> {

    public final AnnotationMirror GTEN1, NN, POS, UNKNOWN;
    private final ValueAnnotatedTypeFactory valueAnnotatedTypeFactory;

    public LowerBoundAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        GTEN1 = AnnotationUtils.fromClass(elements, GTENegativeOne.class);
        NN = AnnotationUtils.fromClass(elements, NonNegative.class);
        POS = AnnotationUtils.fromClass(elements, Positive.class);
        UNKNOWN = AnnotationUtils.fromClass(elements, LowerBoundUnknown.class);
        valueAnnotatedTypeFactory = getTypeFactoryOfSubchecker(ValueChecker.class);
        this.postInit();
    }

    @Override
    protected LowerBoundAnalysis createFlowAnalysis(
            List<Pair<VariableElement, CFValue>> fieldValues) {
        return new LowerBoundAnalysis(checker, this, fieldValues);
    }

    /** Suzanne's hack to make postfix increments and decrements work correctly
     *  We think this is just getting around an actual bug in the framework
     */
    @Override
    public AnnotatedTypeMirror getAnnotatedType(Tree tree) {
        if (tree.getKind() == Tree.Kind.POSTFIX_DECREMENT
                || tree.getKind() == Tree.Kind.POSTFIX_INCREMENT) {

            return getAnnotatedType(((UnaryTree) tree).getExpression());
        } else {
            return super.getAnnotatedType(tree);
        }
    }


    /* I moved these out of the tree annotator because reasons */

    /** handles x+1, ++x, and all equivalent statements */
    public void incrementHelper(AnnotatedTypeMirror leftType, AnnotatedTypeMirror type) {
        if (leftType.hasAnnotation(GTEN1)) {
            type.replaceAnnotation(NN);
        } else if (leftType.hasAnnotation(NN)) {
            type.replaceAnnotation(POS);
        } else if (leftType.hasAnnotation(POS)) {
            type.replaceAnnotation(POS);
        } else {
            type.replaceAnnotation(UNKNOWN);
        }
        return;
    }

    /** handles x-1, --x, and all equivalent statements */
    public void decrementHelper(AnnotatedTypeMirror leftType, AnnotatedTypeMirror type) {
        if (leftType.hasAnnotation(NN)) {
            type.replaceAnnotation(GTEN1);
        } else if (leftType.hasAnnotation(POS)) {
            type.replaceAnnotation(NN);
        } else {
            type.replaceAnnotation(UNKNOWN);
        }
        return;
    }

    private AnnotatedTypeMirror getPostFixAnnoIncrement(AnnotatedTypeMirror anno) {
        if (anno.hasAnnotation(GTEN1)) {
            anno.replaceAnnotation(NN);
        } else if (anno.hasAnnotation(NN)) {
            anno.replaceAnnotation(POS);
        } else if (anno.hasAnnotation(POS)) {
            anno.replaceAnnotation(POS);
        } else {
            anno.replaceAnnotation(UNKNOWN);
        }
        return anno;
    }

    private AnnotatedTypeMirror getPostFixAnnoDecrement(AnnotatedTypeMirror anno) {
        if (anno.hasAnnotation(GTEN1)) {
            anno.replaceAnnotation(UNKNOWN);
        } else if (anno.hasAnnotation(NN)) {
            anno.replaceAnnotation(GTEN1);
        } else if (anno.hasAnnotation(POS)) {
            anno.replaceAnnotation(NN);
        } else {
            anno.replaceAnnotation(UNKNOWN);
        }
        return anno;
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                new LowerBoundTreeAnnotator(this),
                new PropagationTreeAnnotator(this)
        );
    }

    private class LowerBoundTreeAnnotator extends TreeAnnotator{
        public LowerBoundTreeAnnotator(AnnotatedTypeFactory annotatedTypeFactory) {
            super(annotatedTypeFactory);
        }

        /** does the actual work of annotating a literal so that we can call this from
            elsewhere in this program (e.g. computeTypesForPlus w/ two literals) */
        private void literalHelper(int val, AnnotatedTypeMirror type) {
            if (val == -1) {
                type.addAnnotation(GTEN1);
            } else if (val == 0) {
                type.addAnnotation(NN);
            } else if (val > 0) {
                type.addAnnotation(POS);
            } else {
                type.addAnnotation(UNKNOWN);
            }
        }

        /** we could call the constant value checker here but if we already know its a literal... */
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
                if (leftType == null) {
                    leftType = getAnnotatedType(tree.getExpression());
                }
                incrementHelper(leftType, type);
                break;
            case PREFIX_DECREMENT:
                if (leftType == null) {
                     leftType = getAnnotatedType(tree.getExpression());
                }
                decrementHelper(leftType, type);
                break;
            case POSTFIX_INCREMENT:
                break;
            case POSTFIX_DECREMENT:
                break;
            default:
                break;
            }
            Void v = super.visitUnary(tree, type);
            return v;
        }

        /** get the list of possible values from a value checker type */
        private List<Long> possibleValuesFromValueType(AnnotatedTypeMirror valueType) {
            List<Long> possibleValues = null;
            try {
                possibleValues =
                    ValueAnnotatedTypeFactory.getIntValues(valueType.getAnnotation(IntVal.class));
            } catch (NullPointerException npe) {}
            return possibleValues;
        }

        /** @throws IllegalArgumentException
            returns a single value equal to what the value checker believes the value
            of the argument is. If the value checker cannot determine the exact value
            of the input, throw an Illegal argument exception */
        public int intFromValueType(AnnotatedTypeMirror valueType) {
            List<Long> possibleValues = possibleValuesFromValueType(valueType);
            if (possibleValues != null && possibleValues.size() == 1) {
                return possibleValues.get(0).intValue();
            } else {
                throw new IllegalArgumentException();
            }
        }

        /** figure out which type in our hierarchy a value checker type corresponds to */
        public AnnotationMirror lowerBoundTypeFromValueType(AnnotatedTypeMirror valueType) {
            List<Long> possibleValues = possibleValuesFromValueType(valueType);
            if (possibleValues == null || possibleValues.size() == 0) {
                return UNKNOWN;
            }
            boolean fPos = true;
            boolean fNN = true;
            for (Long l : possibleValues) {
                if (l < -1) {
                    return UNKNOWN;
                } else if (l < 0) {
                    fPos = false;
                    fNN = false;
                } else if (l < 1) {
                    fPos = false;
                }
            }
            // if we've made it this far, the list contains no negative values
            if (possibleValues.size() > 0) {
                if (fPos) {
                    return POS;
                } else if (fNN) {
                    return NN;
                } else {
                    return GTEN1;
                }
            }
            return UNKNOWN;
        }

        /** if the valueType pins the variable into a single qualifier, return true and assign
         * the ATM into that qualifier. otherwise, return false
         */
        public boolean valueCheckerCanAssignType(AnnotatedTypeMirror valueType,
                                                 AnnotatedTypeMirror type) {
            AnnotationMirror anm = lowerBoundTypeFromValueType(valueType);
            if (anm != UNKNOWN) {
                type.addAnnotation(anm);
                return true;
            }
            return false;
        }

        /** dispatch to binary operator helper methods. the lower bound checker currently
         *  handles addition, subtraction, multiplication, division, and modular division */
        @Override
        public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
            AnnotatedTypeMirror valueType = valueAnnotatedTypeFactory.getAnnotatedType(tree);
            if (valueCheckerCanAssignType(valueType, type)) {
                return super.visitBinary(tree, type);
            }

            ExpressionTree left = tree.getLeftOperand();
            ExpressionTree right = tree.getRightOperand();
            switch (tree.getKind()) {
            case PLUS:
                computeTypesForPlus(left, right, type);
                break;
            case MINUS:
                computeTypesForMinus(left, right, type);
                break;
            case MULTIPLY:
                computeTypesForTimes(left, right, type);
                break;
            case DIVIDE:
                computeTypesForDivide(left, right, type);
                break;
            case REMAINDER:
                computeTypesForMod(left, right, type);
                break;
            default:
                break;
            }
            return super.visitBinary(tree, type);
        }

        private void computeTypesForLiteralPlus(int val, AnnotatedTypeMirror leftType,
                                                 AnnotatedTypeMirror type) {
            if (val == -2) {
                if (leftType.hasAnnotation(POS)) {
                    type.addAnnotation(GTEN1);
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
                if (leftType.hasAnnotation(GTEN1) || leftType.hasAnnotation(NN) ||
                    leftType.hasAnnotation(POS)) {
                    type.addAnnotation(POS);
                    return;
                }
            }
        }

       /**   computeTypesForPlus handles the following cases:
        *       int lit + int lit -> do the math
        *       lit 0 + * -> *
        *       lit 1 + * -> call increment
        *       lit -1 + * -> call decrement
        *       lit >= 2 + gten1, nn, or pos -> pos
        *       lit -2 + pos -> gten1
        *       let all other lits fall through:
        *       pos + pos -> pos
        *       pos + nn -> pos
        *       nn + nn -> nn
        *       pos + gten1 -> nn
        *       nn + gten1 -> gten1
        *      * + * -> lbu
        */

        public void computeTypesForPlus(ExpressionTree leftExpr, ExpressionTree rightExpr,
                               AnnotatedTypeMirror type) {
            AnnotatedTypeMirror leftType = getAnnotatedType(leftExpr);
            // check if the right side's value is known at compile time
            try {
                AnnotatedTypeMirror valueType = valueAnnotatedTypeFactory.getAnnotatedType(rightExpr);
                int val = intFromValueType(valueType);
                computeTypesForLiteralPlus(val, leftType, type);
                return;
            } catch (IllegalArgumentException iae) {}

            AnnotatedTypeMirror rightType = getAnnotatedType(rightExpr);
            // check if the left side's value is known at compile time
            try {
                AnnotatedTypeMirror valueType = valueAnnotatedTypeFactory.getAnnotatedType(leftExpr);
                int val = intFromValueType(valueType);
                computeTypesForLiteralPlus(val, rightType, type);
                return;
            } catch (IllegalArgumentException iae) {}

            /* This section is handling the generic cases:
             pos + pos -> pos
             pos + nn -> pos
             nn + nn -> nn
             pos + gten1 -> nn
             nn + gten1 -> gten1
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
            if ((leftType.hasAnnotation(POS) && rightType.hasAnnotation(GTEN1)) ||
                (leftType.hasAnnotation(GTEN1) && rightType.hasAnnotation(POS))) {
                type.addAnnotation(NN);
                return;
            }
            if ((leftType.hasAnnotation(GTEN1) && rightType.hasAnnotation(NN)) ||
                (leftType.hasAnnotation(NN) && rightType.hasAnnotation(GTEN1))) {
                type.addAnnotation(GTEN1);
                return;
            }

            // * + * -> lbu
            type.addAnnotation(UNKNOWN);
            return;
        }

        private void computeTypesForLiteralMinus(int val, AnnotatedTypeMirror leftType,
                                                   AnnotatedTypeMirror type) {
            if (val == 2) {
                if (leftType.hasAnnotation(POS)) {
                    type.addAnnotation(GTEN1);
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
                return;
            }
            else if (val <= -2) {
                if (leftType.hasAnnotation(GTEN1) || leftType.hasAnnotation(NN) ||
                    leftType.hasAnnotation(POS)) {
                        type.addAnnotation(POS);
                        return;
                }
            }
        }

        /** computeTypesForMinus handles the following cases:
          *     int lit - int lit -> do the math
          *     * - lit 0 -> *
          *     * - lit 1 -> call decrement
          *     * - lit -1 -> call increment
          *     pos - lit 2 -> gten1
          *     gten1, nn, pos - lit <= -2 -> pos
          *     * - * -> lbu
          */
        public void computeTypesForMinus(ExpressionTree leftExpr, ExpressionTree rightExpr,
                               AnnotatedTypeMirror type) {
            AnnotatedTypeMirror leftType = getAnnotatedType(leftExpr);
            // check if the right side's value is known at compile time
            try {
                AnnotatedTypeMirror valueType = valueAnnotatedTypeFactory.getAnnotatedType(rightExpr);
                int val = intFromValueType(valueType);
                computeTypesForLiteralMinus(val, leftType, type);
                return;
            } catch (IllegalArgumentException iae) {}

            // we can't say anything about generic things that are being subtracted, sadly
            type.addAnnotation(UNKNOWN);
            return;
        }

        private void computeTypesForLiteralTimes(int val, AnnotatedTypeMirror leftType,
                                                 AnnotatedTypeMirror type) {
            if (val == 0) {
                type.addAnnotation(NN);
                return;
            } else if (val == 1) {
                type.addAnnotation(leftType.getAnnotationInHierarchy(POS));
                return;
            }
        }

        /**
         *      computeTypesForTimes handles the following cases:
         *        int lit * int lit -> do the math
         *        * * lit 0 -> nn (=0)
         *        * * lit 1 -> *
         *        pos * pos -> pos
         *        pos * nn -> nn
         *        nn * nn -> nn
         *        * * * -> lbu
         */
        public void computeTypesForTimes(ExpressionTree leftExpr, ExpressionTree rightExpr,
                               AnnotatedTypeMirror type) {
            AnnotatedTypeMirror leftType = getAnnotatedType(leftExpr);
            // check if the right side's value is known at compile time
            try {
                AnnotatedTypeMirror valueType = valueAnnotatedTypeFactory.getAnnotatedType(rightExpr);
                int val = intFromValueType(valueType);
                computeTypesForLiteralTimes(val, leftType, type);
                return;
            } catch (IllegalArgumentException iae) {}

            AnnotatedTypeMirror rightType = getAnnotatedType(rightExpr);
            // check if the left side's value is known at compile time
            try {
                AnnotatedTypeMirror valueType = valueAnnotatedTypeFactory.getAnnotatedType(leftExpr);
                int val = intFromValueType(valueType);
                computeTypesForLiteralTimes(val, rightType, type);
                return;
            } catch (IllegalArgumentException iae) {}

            /* this section handles generic annotations
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

        /** when the value on the left is known at compile time */
        private void computeTypesForLiteralDivideLeft(int val, AnnotatedTypeMirror rightType,
                                                  AnnotatedTypeMirror type) {
            if (val == 0) {
                type.addAnnotation(NN);
                return;
            } else if (val == 1) {
                if (rightType.hasAnnotation(GTEN1)) {
                    type.addAnnotation(GTEN1);
                } else if (rightType.hasAnnotation(NN) || rightType.hasAnnotation(POS)) {
                    type.addAnnotation(NN);
                }
                return;
            }
        }

        /** @throws ArithmeticException
         *   when the value on the right is known at compile time
         *   if the value is zero, then we've discovered division by zero and we throw an exception
         */
        private void computeTypesForLiteralDivideRight(int val, AnnotatedTypeMirror leftType,
                                                  AnnotatedTypeMirror type) {
            if (val == 0) {
                // if we get here then this is a divide by zero error...
                throw new ArithmeticException();
            } else if (val == 1) {
                type.addAnnotation(leftType.getAnnotationInHierarchy(POS));
                return;
            }
        }

        /**
         *      int lit / int lit -> do the math
         *      lit 0 / * -> nn
         *      * / lit 1 -> *
         *      pos / pos -> nn
         *      nn / pos -> nn
         *      pos / nn -> nn
         *      nn / nn -> nn
         *      pos / gten1 -> gten1
         *      nn / gten1 -> gten1
         *      gten1 / gten1 -> nn
         *      * / * -> lbu
         */
        public void computeTypesForDivide(ExpressionTree leftExpr, ExpressionTree rightExpr,
                               AnnotatedTypeMirror type) {
            AnnotatedTypeMirror rightType = getAnnotatedType(rightExpr);
            // check if the left side's value is known at compile time
            try {
                AnnotatedTypeMirror valueType = valueAnnotatedTypeFactory.getAnnotatedType(leftExpr);
                int val = intFromValueType(valueType);
                computeTypesForLiteralDivideLeft(val, rightType, type);
                return;
            } catch (IllegalArgumentException iae) {}

            AnnotatedTypeMirror leftType = getAnnotatedType(leftExpr);
            // check if the right side's value is known at compile time
            try {
                AnnotatedTypeMirror valueType = valueAnnotatedTypeFactory.getAnnotatedType(rightExpr);
                int val = intFromValueType(valueType);
                computeTypesForLiteralDivideRight(val, leftType, type);
                return;
            } catch (IllegalArgumentException iae) {}

            /* this section handles generic annotations
               pos / pos -> nn
               nn / pos -> nn
               pos / nn -> nn
               nn / nn -> nn
               gten1 / pos -> gten1
               gten1 / nn -> gten1
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
            if (leftType.hasAnnotation(GTEN1) && rightType.hasAnnotation(POS)) {
                type.addAnnotation(GTEN1);
                return;
            }
            if (leftType.hasAnnotation(GTEN1) && rightType.hasAnnotation(NN)) {
                type.addAnnotation(GTEN1);
                return;
            }
            // we don't know anything about other stuff.
            type.addAnnotation(UNKNOWN);
            return;
        }

        private void computeTypesForLiteralMod(int val, AnnotatedTypeMirror type) {
            if (val == 1 || val == -1) {
                type.addAnnotation(NN);
                return;
            }
        }

        /**
         *  int lit % int lit -> do the math
         *  * % 1/-1 -> nn
         *  pos/nn % * -> nn
         *  gten1 % * -> gten1
         *  * % * -> lbu
         */
        public void computeTypesForMod(ExpressionTree leftExpr, ExpressionTree rightExpr,
                               AnnotatedTypeMirror type) {
            AnnotatedTypeMirror leftType = getAnnotatedType(leftExpr);
            AnnotatedTypeMirror rightType = getAnnotatedType(rightExpr);

            // check if the right side's value is known at compile time
            try {
                AnnotatedTypeMirror valueType = valueAnnotatedTypeFactory.getAnnotatedType(rightExpr);
                int val = intFromValueType(valueType);
                computeTypesForLiteralMod(val, type);
                return;
            } catch (IllegalArgumentException iae) {}

            /* this section handles generic annotations
                pos/nn % * -> nn
                gten1 % * -> gten1
             */
            if (leftType.hasAnnotation(POS) || leftType.hasAnnotation(NN)) {
                type.addAnnotation(NN);
                return;
            }
            if (leftType.hasAnnotation(GTEN1)) {
                type.addAnnotation(GTEN1);
                return;
            }

            // we don't know anything about other stuff.
            type.addAnnotation(UNKNOWN);
            return;
        }
    }
}
