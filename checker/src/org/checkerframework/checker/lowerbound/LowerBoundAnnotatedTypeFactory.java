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
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.ValueChecker;
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
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.Pair;

/** Contains all of the introduction rules for the Lower Bound Checker.
 *  The rules this class implements are found in lowerbound_rules.txt,
 *  in the same directory in the source tree.
 */
public class LowerBoundAnnotatedTypeFactory
        extends GenericAnnotatedTypeFactory<
                CFValue, CFStore, LowerBoundTransfer, LowerBoundAnalysis> {

    /* Since these don't take arguments, we only need one version of each */
    public final AnnotationMirror GTEN1, NN, POS, UNKNOWN;

    /* a single link to the value checker, which computes the values
    of expressions known at compile time (constant prop + folding) */
    private final ValueAnnotatedTypeFactory valueAnnotatedTypeFactory;

    public LowerBoundAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        // initialize the individual annotations
        GTEN1 = AnnotationUtils.fromClass(elements, GTENegativeOne.class);
        NN = AnnotationUtils.fromClass(elements, NonNegative.class);
        POS = AnnotationUtils.fromClass(elements, Positive.class);
        UNKNOWN = AnnotationUtils.fromClass(elements, LowerBoundUnknown.class);
        /* the value checker needs to run before this checker, because
        this line is querying it */
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
            // TODO: This is a workaround for Issue 867
            // https://github.com/typetools/checker-framework/issues/867
            AnnotatedTypeMirror refinedType = super.getAnnotatedType(tree);
            AnnotatedTypeMirror typeOfExpression =
                    getAnnotatedType(((UnaryTree) tree).getExpression());
            // Call asSuper in case the type of decrement tree is different than the type of it's
            // expression
            return AnnotatedTypes.asSuper(this, typeOfExpression, typeOfExpression);
        } else {
            return super.getAnnotatedType(tree);
        }
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                new LowerBoundTreeAnnotator(this), new PropagationTreeAnnotator(this));
    }

    private class LowerBoundTreeAnnotator extends TreeAnnotator {
        public LowerBoundTreeAnnotator(AnnotatedTypeFactory annotatedTypeFactory) {
            super(annotatedTypeFactory);
        }

        /** does the actual work of annotating a literal so that we can call this from
         * elsewhere in this program (e.g. computeTypesForPlus w/ two literals) */
        private void computeTypesForLiterals(int val, AnnotatedTypeMirror type) {
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

        /** sets typeDst to one higher in the hierarchy than typeSrc */
        public void promoteType(AnnotatedTypeMirror typeSrc, AnnotatedTypeMirror typeDst) {
            if (typeSrc.hasAnnotation(GTEN1)) {
                typeDst.replaceAnnotation(NN);
            } else if (typeSrc.hasAnnotation(NN)) {
                typeDst.replaceAnnotation(POS);
            } else if (typeSrc.hasAnnotation(POS)) {
                typeDst.replaceAnnotation(POS);
            } else {
                typeDst.replaceAnnotation(UNKNOWN);
            }
            return;
        }

        /** sets typeDst to one lower in the hierarchy than typeSrc */
        public void demoteType(AnnotatedTypeMirror typeSrc, AnnotatedTypeMirror typeDst) {
            if (typeSrc.hasAnnotation(NN)) {
                typeDst.replaceAnnotation(GTEN1);
            } else if (typeSrc.hasAnnotation(POS)) {
                typeDst.replaceAnnotation(NN);
            } else {
                typeDst.replaceAnnotation(UNKNOWN);
            }
            return;
        }

        /** we could call the constant value checker here but if we already know its a literal... */
        @Override
        public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
            /** only annotate integers */
            if (tree.getKind() == Tree.Kind.INT_LITERAL) {
                int val = (int) tree.getValue();
                computeTypesForLiterals(val, type);
            }
            return super.visitLiteral(tree, type);
        }

        /** call increment and decrement helper functions */
        @Override
        public Void visitUnary(UnaryTree tree, AnnotatedTypeMirror typeDst) {
            AnnotatedTypeMirror typeSrc = getAnnotatedType(tree.getExpression());
            switch (tree.getKind()) {
                case PREFIX_INCREMENT:
                    if (typeSrc == null) {
                        typeSrc = getAnnotatedType(tree.getExpression());
                    }
                    promoteType(typeSrc, typeDst);
                    break;
                case PREFIX_DECREMENT:
                    if (typeSrc == null) {
                        typeSrc = getAnnotatedType(tree.getExpression());
                    }
                    demoteType(typeSrc, typeDst);
                    break;
                case POSTFIX_INCREMENT: // do nothing. The hack above takes care of these.
                    break;
                case POSTFIX_DECREMENT:
                    break;
                default:
                    break;
            }
            return super.visitUnary(tree, typeDst);
        }

        /** get the list of possible values from a value checker type.
         * May return null. */
        private List<Long> possibleValuesFromValueType(AnnotatedTypeMirror valueType) {
            List<Long> possibleValues = null;
            AnnotationMirror anm = valueType.getAnnotation(IntVal.class);
            if (anm == null) {
                return null;
            }
            possibleValues = ValueAnnotatedTypeFactory.getIntValues(anm);
            return possibleValues;
        }

        /** This struct represents an optional integer. */
        private class MaybeVal {
            public int val;
            public boolean fValid;

            public MaybeVal(int v, boolean f) {
                val = v;
                fValid = f;
            }
        }

        /**
         * Returns a struct containing an integer equal to what the value checker believes the value
         * of the argument is. If the value checker cannot determine the exact value
         * of the input, the struct also contains a flag telling the caller not to use the
         * value.
         */
        public MaybeVal maybeValFromValueType(AnnotatedTypeMirror valueType) {
            List<Long> possibleValues = possibleValuesFromValueType(valueType);
            if (possibleValues != null && possibleValues.size() == 1) {
                return new MaybeVal(possibleValues.get(0).intValue(), true);
            } else {
                return new MaybeVal(-10, false); // totally arbitrary value
            }
        }

        /** Figure out which type in the lower bound hierarchy a value checker type corresponds to.
         *  Returns an annotation mirror. In the code, AnnotationMirror is abbr. as anm.
         */
        public AnnotationMirror lowerBoundAnmFromValueType(AnnotatedTypeMirror valueType) {
            List<Long> possibleValues = possibleValuesFromValueType(valueType);
            if (possibleValues == null || possibleValues.size() == 0) {
                return UNKNOWN;
            }
            boolean fPos = true;
            boolean fNN = true;
            for (Long possibleValue : possibleValues) {
                if (possibleValue < -1) {
                    /* if the value checker finds any value < -1, we won't be able to say
                    anything interesting about this variable: it might be < -1 */
                    return UNKNOWN;
                }
                if (possibleValue < 0) {
                    fNN = false;
                }
                if (possibleValue < 1) {
                    fPos = false;
                }
            }
            // if we've made it this far, the list contains no negative values
            if (fPos) {
                return POS;
            } else if (fNN) {
                return NN;
            } else {
                return GTEN1;
            }
        }

        /** dispatch to binary operator helper methods. the lower bound checker currently
         *  handles addition, subtraction, multiplication, division, and modular division */
        @Override
        public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
            // see if the value checker's information puts it into a single type
            AnnotatedTypeMirror valueType = valueAnnotatedTypeFactory.getAnnotatedType(tree);
            AnnotationMirror lowerBoundAnm = lowerBoundAnmFromValueType(valueType);
            if (lowerBoundAnm != UNKNOWN) {
                type.addAnnotation(lowerBoundAnm);
                return super.visitBinary(tree, type);
            }

            // dispatch according to the operation
            ExpressionTree left = tree.getLeftOperand();
            ExpressionTree right = tree.getRightOperand();
            // every "computeTypesForX" method is stateful and modifies type
            switch (tree.getKind()) {
                case PLUS:
                    computeTypesForPlus(left, right, type);
                    break;
                case MINUS:
                    computeTypesForMinus(left, right, type);
                    break;
                case MULTIPLY:
                    computeTypesForMultiply(left, right, type);
                    break;
                case DIVIDE:
                    computeTypesForDivide(left, right, type);
                    break;
                case REMAINDER:
                    computeTypesForRemainder(left, right, type);
                    break;
                default:
                    break;
            }
            return super.visitBinary(tree, type);
        }

        /** helper method for computeTypesForPlus. Handles addition of constants */
        private void computeTypesForLiteralPlus(
                int val, AnnotatedTypeMirror leftType, AnnotatedTypeMirror type) {
            if (val == -2) {
                if (leftType.hasAnnotation(POS)) {
                    type.addAnnotation(GTEN1);
                    return;
                }
            } else if (val == -1) {
                demoteType(leftType, type);
                return;
            } else if (val == 0) {
                type.addAnnotation(leftType.getAnnotationInHierarchy(POS));
                return;
            } else if (val == 1) {
                promoteType(leftType, type);
                return;
            } else if (val >= 2) {
                if (leftType.hasAnnotation(GTEN1)
                        || leftType.hasAnnotation(NN)
                        || leftType.hasAnnotation(POS)) {
                    type.addAnnotation(POS);
                    return;
                }
            }
        }

        /**   computeTypesForPlus handles the following cases:
         *       lit 0 + * becomes *
         *       lit 1 + * becomes call increment
         *       lit -1 + * becomes call decrement
         *       lit greater than or equal to 2 + gten1, nn, or pos becomes pos
         *       lit -2 + pos becomes gten1
         *       let all other lits fall through:
         *       pos + pos becomes pos
         *       pos + nn becomes pos
         *       nn + nn becomes nn
         *       pos + gten1 becomes nn
         *       nn + gten1 becomes gten1
         *      * + * becomes lbu
         */
        public void computeTypesForPlus(
                ExpressionTree leftExpr, ExpressionTree rightExpr, AnnotatedTypeMirror type) {

            AnnotatedTypeMirror leftType = getAnnotatedType(leftExpr);
            // check if the right side's value is known at compile time
            AnnotatedTypeMirror valueTypeRight =
                    valueAnnotatedTypeFactory.getAnnotatedType(rightExpr);
            MaybeVal maybeValRight = maybeValFromValueType(valueTypeRight);
            if (maybeValRight.fValid) {
                int val = maybeValRight.val;
                computeTypesForLiteralPlus(val, leftType, type);
                return;
            }

            AnnotatedTypeMirror rightType = getAnnotatedType(rightExpr);
            // check if the left side's value is known at compile time
            AnnotatedTypeMirror valueTypeLeft =
                    valueAnnotatedTypeFactory.getAnnotatedType(rightExpr);
            MaybeVal maybeValLeft = maybeValFromValueType(valueTypeLeft);
            if (maybeValLeft.fValid) {
                int val = maybeValLeft.val;
                computeTypesForLiteralPlus(val, rightType, type);
                return;
            }

            /* This section is handling the generic cases:
             pos + pos becomes pos
             pos + nn becomes pos
             nn + nn becomes nn
             pos + gten1 becomes nn
             nn + gten1 becomes gten1
            */
            if (leftType.hasAnnotation(POS) && rightType.hasAnnotation(POS)) {
                type.addAnnotation(POS);
                return;
            }
            if ((leftType.hasAnnotation(POS) && rightType.hasAnnotation(NN))
                    || (leftType.hasAnnotation(NN) && rightType.hasAnnotation(POS))) {
                type.addAnnotation(POS);
                return;
            }
            if (leftType.hasAnnotation(NN) && rightType.hasAnnotation(NN)) {
                type.addAnnotation(NN);
                return;
            }
            if ((leftType.hasAnnotation(POS) && rightType.hasAnnotation(GTEN1))
                    || (leftType.hasAnnotation(GTEN1) && rightType.hasAnnotation(POS))) {
                type.addAnnotation(NN);
                return;
            }
            if ((leftType.hasAnnotation(GTEN1) && rightType.hasAnnotation(NN))
                    || (leftType.hasAnnotation(NN) && rightType.hasAnnotation(GTEN1))) {
                type.addAnnotation(GTEN1);
                return;
            }

            // * + * becomes lbu
            type.addAnnotation(UNKNOWN);
            return;
        }

        /** helper function for computeTypesForMinus. Handles compile-time known constants */
        private void computeTypesForLiteralMinus(
                int val, AnnotatedTypeMirror leftType, AnnotatedTypeMirror type) {
            if (val == 2) {
                if (leftType.hasAnnotation(POS)) {
                    type.addAnnotation(GTEN1);
                    return;
                }
            } else if (val == 1) {
                demoteType(leftType, type);
                return;
            } else if (val == 0) {
                type.addAnnotation(leftType.getAnnotationInHierarchy(POS));
                return;
            } else if (val == -1) {
                promoteType(leftType, type);
                return;
            } else if (val <= -2) {
                if (leftType.hasAnnotation(GTEN1)
                        || leftType.hasAnnotation(NN)
                        || leftType.hasAnnotation(POS)) {
                    type.addAnnotation(POS);
                    return;
                }
            }
        }

        /** computeTypesForMinus handles the following cases:
         *     * - lit 0 becomes *
         *     * - lit 1 becomes call decrement
         *     * - lit -1 becomes call increment
         *     pos - lit 2 becomes gten1
         *     gten1, nn, pos - lit less than or equal to -2 becomes pos
         *     * - * becomes lbu
         */
        public void computeTypesForMinus(
                ExpressionTree leftExpr, ExpressionTree rightExpr, AnnotatedTypeMirror type) {

            AnnotatedTypeMirror leftType = getAnnotatedType(leftExpr);
            // check if the right side's value is known at compile time
            AnnotatedTypeMirror valueTypeRight =
                    valueAnnotatedTypeFactory.getAnnotatedType(rightExpr);
            MaybeVal maybeValRight = maybeValFromValueType(valueTypeRight);
            if (maybeValRight.fValid) {
                int val = maybeValRight.val;
                computeTypesForLiteralMinus(val, leftType, type);
                return;
            }

            // we can't say anything about generic things that are being subtracted, sadly
            type.addAnnotation(UNKNOWN);
            return;
        }

        /** helper function for computeTypesForMultiply. Handles compile-time known constants */
        private void computeTypesForLiteralMultiply(
                int val, AnnotatedTypeMirror leftType, AnnotatedTypeMirror type) {
            if (val == 0) {
                type.addAnnotation(NN);
                return;
            } else if (val == 1) {
                type.addAnnotation(leftType.getAnnotationInHierarchy(POS));
                return;
            }
        }

        /**
         *      computeTypesForMultiply handles the following cases:
         *        * * lit 0 becomes nn (=0)
         *        * * lit 1 becomes *
         *        pos * pos becomes pos
         *        pos * nn becomes nn
         *        nn * nn becomes nn
         *        * * * becomes lbu
         */
        public void computeTypesForMultiply(
                ExpressionTree leftExpr, ExpressionTree rightExpr, AnnotatedTypeMirror type) {

            AnnotatedTypeMirror leftType = getAnnotatedType(leftExpr);
            // check if the right side's value is known at compile time
            AnnotatedTypeMirror valueTypeRight =
                    valueAnnotatedTypeFactory.getAnnotatedType(rightExpr);
            MaybeVal maybeValRight = maybeValFromValueType(valueTypeRight);
            if (maybeValRight.fValid) {
                int val = maybeValRight.val;
                computeTypesForLiteralMultiply(val, leftType, type);
                return;
            }

            AnnotatedTypeMirror rightType = getAnnotatedType(rightExpr);
            // check if the left side's value is known at compile time
            AnnotatedTypeMirror valueTypeLeft =
                    valueAnnotatedTypeFactory.getAnnotatedType(leftExpr);
            MaybeVal maybeValLeft = maybeValFromValueType(valueTypeLeft);
            if (maybeValLeft.fValid) {
                int val = maybeValLeft.val;
                computeTypesForLiteralMultiply(val, rightType, type);
                return;
            }

            /* this section handles generic annotations
                pos * pos becomes pos
                nn * pos becomes nn
                nn * nn becomes nn
            */
            if (leftType.hasAnnotation(POS) && rightType.hasAnnotation(POS)) {
                type.addAnnotation(POS);
                return;
            }
            if ((leftType.hasAnnotation(POS) && rightType.hasAnnotation(NN))
                    || (leftType.hasAnnotation(NN) && rightType.hasAnnotation(POS))) {
                type.addAnnotation(NN);
                return;
            }
            if (leftType.hasAnnotation(NN) && rightType.hasAnnotation(NN)) {
                type.addAnnotation(NN);
                return;
            }
        }

        /** when the value on the left is known at compile time */
        private void computeTypesForLiteralDivideLeft(
                int val, AnnotatedTypeMirror rightType, AnnotatedTypeMirror type) {
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
        private void computeTypesForLiteralDivideRight(
                int val, AnnotatedTypeMirror leftType, AnnotatedTypeMirror type) {
            if (val == 0) {
                // if we get here then this is a divide by zero error...
                // TODO: I'm not convinced this is the right behavior, but
                // I'm unsure of what's correct here.
                throw new ArithmeticException();
            } else if (val == 1) {
                type.addAnnotation(leftType.getAnnotationInHierarchy(POS));
                return;
            }
        }

        /**     computeTypesForDivide handles these cases:
         *      lit 0 / * becomes nn
         *      * / lit 1 becomes *
         *      pos / pos becomes nn
         *      nn / pos becomes nn
         *      pos / nn becomes nn
         *      nn / nn becomes nn
         *      pos / gten1 becomes gten1
         *      nn / gten1 becomes gten1
         *      gten1 / gten1 becomes nn
         *      * / * becomes lbu
         */
        public void computeTypesForDivide(
                ExpressionTree leftExpr, ExpressionTree rightExpr, AnnotatedTypeMirror type) {

            AnnotatedTypeMirror leftType = getAnnotatedType(leftExpr);
            // check if the right side's value is known at compile time
            AnnotatedTypeMirror valueTypeRight =
                    valueAnnotatedTypeFactory.getAnnotatedType(rightExpr);
            MaybeVal maybeValRight = maybeValFromValueType(valueTypeRight);
            if (maybeValRight.fValid) {
                int val = maybeValRight.val;
                computeTypesForLiteralDivideRight(val, leftType, type);
                return;
            }

            AnnotatedTypeMirror rightType = getAnnotatedType(rightExpr);
            // check if the left side's value is known at compile time
            AnnotatedTypeMirror valueTypeLeft =
                    valueAnnotatedTypeFactory.getAnnotatedType(leftExpr);
            MaybeVal maybeValLeft = maybeValFromValueType(valueTypeLeft);
            if (maybeValLeft.fValid) {
                int val = maybeValLeft.val;
                computeTypesForLiteralDivideLeft(val, leftType, type);
                return;
            }

            /* this section handles generic annotations
               pos / pos becomes nn
               nn / pos becomes nn
               pos / nn becomes nn
               nn / nn becomes nn
               gten1 / pos becomes gten1
               gten1 / nn becomes gten1
            */
            if (leftType.hasAnnotation(POS) && rightType.hasAnnotation(POS)) {
                type.addAnnotation(NN);
                return;
            }
            if ((leftType.hasAnnotation(POS) && rightType.hasAnnotation(NN))
                    || (leftType.hasAnnotation(NN) && rightType.hasAnnotation(POS))) {
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

        private void computeTypesForLiteralRemainder(int val, AnnotatedTypeMirror type) {
            if (val == 1 || val == -1) {
                type.addAnnotation(NN);
                return;
            }
        }

        /**
         *  int lit % int lit becomes do the math
         *  * % 1/-1 becomes nn
         *  pos/nn % * becomes nn
         *  gten1 % * becomes gten1
         *  * % * becomes lbu
         */
        public void computeTypesForRemainder(
                ExpressionTree leftExpr, ExpressionTree rightExpr, AnnotatedTypeMirror type) {
            AnnotatedTypeMirror leftType = getAnnotatedType(leftExpr);
            AnnotatedTypeMirror rightType = getAnnotatedType(rightExpr);

            // check if the right side's value is known at compile time
            AnnotatedTypeMirror valueTypeRight =
                    valueAnnotatedTypeFactory.getAnnotatedType(rightExpr);
            MaybeVal maybeValRight = maybeValFromValueType(valueTypeRight);
            if (maybeValRight.fValid) {
                int val = maybeValRight.val;
                computeTypesForLiteralRemainder(val, type);
                return;
            }

            /* this section handles generic annotations
               pos/nn % * becomes nn
               gten1 % * becomes gten1
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
