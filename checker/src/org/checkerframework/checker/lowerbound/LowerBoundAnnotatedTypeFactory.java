package org.checkerframework.checker.lowerbound;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import java.util.Collections;
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

/**
 *  Implements the introduction rules for the Lower Bound Checker.
 *  <pre>
 *  The type hierarchy is:
 *
 *  Top = lbu ("Lower Bound Unknown")
 *   |
 *  gte-1 ("Greater than or equal to -1")
 *   |
 *  nn  ("NonNegative")
 *   |
 *  pos ("Positive")
 *  </pre>
 *  In general, check whether the constant Value Checker can determine the
 *  value of a variable; if it can, use that; if not, use more specific rules
 *  based on expression type. These rules are documented on the functions
 *  implementing them.
 */
public class LowerBoundAnnotatedTypeFactory
        extends GenericAnnotatedTypeFactory<
                CFValue, CFStore, LowerBoundTransfer, LowerBoundAnalysis> {

    /** The canonical @GTENegativeOne annotation. */
    public final AnnotationMirror GTEN1 = AnnotationUtils.fromClass(elements, GTENegativeOne.class);
    /** The canonical @Negative annotation. */
    public final AnnotationMirror NN = AnnotationUtils.fromClass(elements, NonNegative.class);
    /** The canonical @Positive annotation. */
    public final AnnotationMirror POS = AnnotationUtils.fromClass(elements, Positive.class);
    /** The canonical @LowerBoundUnknown annotation. */
    public final AnnotationMirror UNKNOWN =
            AnnotationUtils.fromClass(elements, LowerBoundUnknown.class);

    /**
     * Provides a way to query the Constant Value Checker, which computes the
     * values of expressions known at compile time (constant prop + folding).
     */
    private final ValueAnnotatedTypeFactory valueAnnotatedTypeFactory =
            getTypeFactoryOfSubchecker(ValueChecker.class);

    public LowerBoundAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        this.postInit();
    }

    @Override
    protected LowerBoundAnalysis createFlowAnalysis(
            List<Pair<VariableElement, CFValue>> fieldValues) {
        return new LowerBoundAnalysis(checker, this, fieldValues);
    }

    /**
     * Hack to make postfix increments and decrements work correctly.
     * This is a workaround for issue #867:
     * https://github.com/typetools/checker-framework/issues/867
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
            // Call asSuper in case the type of decrement tree is different than the type of its
            // expression.
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

        /**
         *  Sets typeDst to the immediate supertype of typeSrc, unless typeSrc is already
         *  Positive. Implements the following transitions:
         *  <pre>
         *      pos &rarr; pos
         *      nn &rarr; pos
         *      gte-1 &rarr; nn
         *      lbu &rarr; lbu
         *  </pre>
         */
        public void promoteType(AnnotatedTypeMirror typeSrc, AnnotatedTypeMirror typeDst) {
            if (typeSrc.hasAnnotation(POS)) {
                typeDst.replaceAnnotation(POS);
            } else if (typeSrc.hasAnnotation(NN)) {
                typeDst.replaceAnnotation(POS);
            } else if (typeSrc.hasAnnotation(GTEN1)) {
                typeDst.replaceAnnotation(NN);
            } else { //Only unknown is left.
                typeDst.replaceAnnotation(UNKNOWN);
            }
            return;
        }

        /**
         *  Sets typeDst to the immediate subtype of typeSrc, unless typeSrc is already
         *  LowerBoundUnknown. Implements the following transitions:
         *  <pre>
         *       pos &rarr; nn
         *       nn &rarr; gte-1
         *       gte-1, lbu &rarr; lbu
         *  </pre>
         */
        public void demoteType(AnnotatedTypeMirror typeSrc, AnnotatedTypeMirror typeDst) {
            if (typeSrc.hasAnnotation(POS)) {
                typeDst.replaceAnnotation(NN);
            } else if (typeSrc.hasAnnotation(NN)) {
                typeDst.replaceAnnotation(GTEN1);
            } else { // GTEN1 and UNKNOWN both become UNKNOWN.
                typeDst.replaceAnnotation(UNKNOWN);
            }
            return;
        }

        /**
         *  Determine the annotation that should be associated with a literal.
         */
        private AnnotationMirror anmFromVal(int val) {
            if (val == -1) {
                return GTEN1;
            } else if (val == 0) {
                return NN;
            } else if (val > 0) {
                return POS;
            } else {
                return UNKNOWN;
            }
        }

        @Override
        public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
            // We could call the constant Value Checker here but if we already know it's a literal...
            if (tree.getKind() == Tree.Kind.INT_LITERAL
                    || tree.getKind() == Tree.Kind.LONG_LITERAL
                    || tree.getKind() == Tree.Kind.CHAR_LITERAL) {
                int val = (int) tree.getValue();
                type.addAnnotation(anmFromVal(val));
            }
            return super.visitLiteral(tree, type);
        }

        /**
         *  Call increment and decrement helper functions.
         */
        @Override
        public Void visitUnary(UnaryTree tree, AnnotatedTypeMirror typeDst) {
            AnnotatedTypeMirror typeSrc = getAnnotatedType(tree.getExpression());
            switch (tree.getKind()) {
                case PREFIX_INCREMENT:
                    promoteType(typeSrc, typeDst);
                    break;
                case PREFIX_DECREMENT:
                    demoteType(typeSrc, typeDst);
                    break;
                case POSTFIX_INCREMENT: // Do nothing. The hack above takes care of these.
                    break;
                case POSTFIX_DECREMENT:
                    break;
                default:
                    break;
            }
            return super.visitUnary(tree, typeDst);
        }

        /**
         *  Get the list of possible values from a Value Checker type.
         *  May return null.
         */
        private List<Long> possibleValuesFromValueType(AnnotatedTypeMirror valueType) {
            AnnotationMirror anm = valueType.getAnnotation(IntVal.class);
            // Anm can be null if the Value Checker didn't assign an IntVal annotation
            if (anm == null) {
                return null;
            }
            return ValueAnnotatedTypeFactory.getIntValues(anm);
        }

        /**
         * If the argument valueType indicates that the Constant Value
         * Checker knows the exact value of the annotated expression,
         * returns that integer.  Otherwise returns null.
         */
        public Integer maybeValFromValueType(AnnotatedTypeMirror valueType) {
            List<Long> possibleValues = possibleValuesFromValueType(valueType);
            if (possibleValues != null && possibleValues.size() == 1) {
                return new Integer(possibleValues.get(0).intValue());
            } else {
                return null;
            }
        }

        /**
         *  Returns the type in the lower bound hierarchy a Value Checker type corresponds to.
         */
        public AnnotationMirror lowerBoundAnmFromValueType(AnnotatedTypeMirror valueType) {
            // In the code, AnnotationMirror is abbr. as anm.
            List<Long> possibleValues = possibleValuesFromValueType(valueType);
            /*  It's possible that possibleValues could be null (if
             *  there was no Value Checker annotation, I guess, but this
             *  definitely happens in practice) or empty (if the value
             *  checker annotated it with its equivalent of our unknown
             *  annotation.
             */
            if (possibleValues == null || possibleValues.size() == 0) {
                return UNKNOWN;
            }
            // The annotation of the whole list is the min of the list.
            long lvalMin = Collections.min(possibleValues);
            // Turn it into an integer.
            int valMin = (int) Math.max(Math.min(Integer.MAX_VALUE, lvalMin), Integer.MIN_VALUE);
            return anmFromVal(valMin);
        }

        /**
         *  Dispatch to binary operator helper methods. The lower bound checker currently
         *  handles addition, subtraction, multiplication, division, and modular division.
         */
        @Override
        public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
            // Check if the Value Checker's information puts it into a single type.
            AnnotatedTypeMirror valueType = valueAnnotatedTypeFactory.getAnnotatedType(tree);
            AnnotationMirror lowerBoundAnm = lowerBoundAnmFromValueType(valueType);
            if (lowerBoundAnm != UNKNOWN) {
                type.addAnnotation(lowerBoundAnm);
                return super.visitBinary(tree, type);
            }

            // Dispatch according to the operation.
            ExpressionTree left = tree.getLeftOperand();
            ExpressionTree right = tree.getRightOperand();
            // Every "addAnnotationForX" method is stateful and modifies the variable "type".
            switch (tree.getKind()) {
                case PLUS:
                    addAnnotationForPlus(left, right, type);
                    break;
                case MINUS:
                    addAnnotationForMinus(left, right, type);
                    break;
                case MULTIPLY:
                    addAnnotationForMultiply(left, right, type);
                    break;
                case DIVIDE:
                    addAnnotationForDivide(left, right, type);
                    break;
                case REMAINDER:
                    addAnnotationForRemainder(left, right, type);
                    break;
                default:
                    break;
            }
            return super.visitBinary(tree, type);
        }

        /**
         *  Helper method for addAnnotationForPlus. Handles addition of constants.
         *  @param val The integer value of the constant.
         *  @param nonLiteralType The type of the side of the expression that isn't a constant.
         *  @param type The type of the result expression.
         */
        private void addAnnotationForLiteralPlus(
                int val, AnnotatedTypeMirror nonLiteralType, AnnotatedTypeMirror type) {
            if (val == -2) {
                if (nonLiteralType.hasAnnotation(POS)) {
                    type.addAnnotation(GTEN1);
                    return;
                }
            } else if (val == -1) {
                demoteType(nonLiteralType, type);
                return;
            } else if (val == 0) {
                /* This gets the type of nonLiteralType in our
                hierarchy (in which POS is the bottom type). */
                type.addAnnotation(nonLiteralType.getAnnotationInHierarchy(POS));
                return;
            } else if (val == 1) {
                promoteType(nonLiteralType, type);
                return;
            } else if (val >= 2) {
                if (nonLiteralType.hasAnnotation(GTEN1)
                        || nonLiteralType.hasAnnotation(NN)
                        || nonLiteralType.hasAnnotation(POS)) {
                    type.addAnnotation(POS);
                    return;
                }
            }
            type.addAnnotation(UNKNOWN);
        }

        /**
         *  <pre>
         *  addAnnotationForPlus handles the following cases:
         *      lit -2 + pos &rarr; gte-1
         *      lit -1 + * &rarr; call demote
         *      lit 0 + * &rarr; *
         *      lit 1 + * &rarr; call promote
         *      lit &ge; 2 + {gte-1, nn, or pos} &rarr; pos
         *      let all other lits, including sets, fall through:
         *      pos + pos &rarr; pos
         *      nn + * &rarr; *
         *      pos + gte-1 &rarr; nn
         *      * + * &rarr; lbu
         *  </pre>
         */
        public void addAnnotationForPlus(
                ExpressionTree leftExpr, ExpressionTree rightExpr, AnnotatedTypeMirror type) {

            // Adding two literals is handled by visitBinary, so we
            // don't have to worry about that case.
            AnnotatedTypeMirror leftType = getAnnotatedType(leftExpr);
            // Check if the right side's value is known at compile time.
            AnnotatedTypeMirror valueTypeRight =
                    valueAnnotatedTypeFactory.getAnnotatedType(rightExpr);
            Integer maybeValRight = maybeValFromValueType(valueTypeRight);
            if (maybeValRight != null) {
                int val = maybeValRight.intValue();
                addAnnotationForLiteralPlus(val, leftType, type);
                return;
            }

            AnnotatedTypeMirror rightType = getAnnotatedType(rightExpr);
            // Check if the left side's value is known at compile time.
            AnnotatedTypeMirror valueTypeLeft =
                    valueAnnotatedTypeFactory.getAnnotatedType(rightExpr);
            Integer maybeValLeft = maybeValFromValueType(valueTypeLeft);
            if (maybeValLeft != null) {
                int val = maybeValLeft.intValue();
                addAnnotationForLiteralPlus(val, rightType, type);
                return;
            }

            /* This section is handling the generic cases:
             *      pos + pos -> pos
             *      nn + * -> *
             *      pos + gte-1 -> nn
             */
            if (leftType.hasAnnotation(POS) && rightType.hasAnnotation(POS)) {
                type.addAnnotation(POS);
                return;
            }

            if (leftType.hasAnnotation(NN)) {
                type.addAnnotation(rightType.getAnnotationInHierarchy(POS));
                return;
            }
            if (rightType.hasAnnotation(NN)) {
                type.addAnnotation(leftType.getAnnotationInHierarchy(POS));
                return;
            }

            if ((leftType.hasAnnotation(POS) && rightType.hasAnnotation(GTEN1))
                    || (leftType.hasAnnotation(GTEN1) && rightType.hasAnnotation(POS))) {
                type.addAnnotation(NN);
                return;
            }

            // * + * becomes lbu.
            type.addAnnotation(UNKNOWN);
            return;
        }

        /**
         *  <pre>
         *  addAnnotationForMinus handles the following cases:
         *      * - lit &rarr; call plus(*, -1 * the value of the lit)
         *      * - * &rarr; lbu
         *  </pre>
         */
        public void addAnnotationForMinus(
                ExpressionTree leftExpr, ExpressionTree rightExpr, AnnotatedTypeMirror type) {

            // Check if the right side's value is known at compile time.
            AnnotatedTypeMirror valueTypeRight =
                    valueAnnotatedTypeFactory.getAnnotatedType(rightExpr);
            Integer maybeValRight = maybeValFromValueType(valueTypeRight);
            if (maybeValRight != null) {
                AnnotatedTypeMirror leftType = getAnnotatedType(leftExpr);
                int val = maybeValRight.intValue();
                // Instead of a separate method for subtraction, add the negative of a constant.
                addAnnotationForLiteralPlus(-1 * val, leftType, type);
                return;
            }

            // We can't say anything about generic things that are being subtracted, sadly.
            type.addAnnotation(UNKNOWN);
            return;
        }

        /**
         *  Helper function for addAnnotationForMultiply. Handles compile-time known constants.
         *  @param val The integer value of the constant.
         *  @param nonLiteralType The type of the side of the expression that isn't a constant.
         *  @param type The type of the result expression.
         */
        private void addAnnotationForLiteralMultiply(
                int val, AnnotatedTypeMirror nonLiteralType, AnnotatedTypeMirror type) {
            if (val == 0) {
                type.addAnnotation(NN);
                return;
            } else if (val == 1) {
                // Make the result type equal to nonLiteralType.
                type.addAnnotation(nonLiteralType.getAnnotationInHierarchy(POS));
                return;
            } else if (val > 1) {
                if (nonLiteralType.hasAnnotation(POS) || nonLiteralType.hasAnnotation(NN)) {
                    type.addAnnotation(nonLiteralType.getAnnotationInHierarchy(POS));
                    return;
                }
            }
            type.addAnnotation(UNKNOWN);
        }

        /**
         *  <pre>
         *  addAnnotationForMultiply handles the following cases:
         *        * * lit 0 &rarr; nn (=0)
         *        * * lit 1 &rarr; *
         *        pos * pos &rarr; pos
         *        pos * nn &rarr; nn
         *        nn * nn &rarr; nn
         *        * * * &rarr; lbu
         *  </pre>
         */
        public void addAnnotationForMultiply(
                ExpressionTree leftExpr, ExpressionTree rightExpr, AnnotatedTypeMirror type) {

            AnnotatedTypeMirror leftType = getAnnotatedType(leftExpr);
            // Check if the right side's value is known at compile time.
            AnnotatedTypeMirror valueTypeRight =
                    valueAnnotatedTypeFactory.getAnnotatedType(rightExpr);
            Integer maybeValRight = maybeValFromValueType(valueTypeRight);
            if (maybeValRight != null) {
                int val = maybeValRight.intValue();
                addAnnotationForLiteralMultiply(val, leftType, type);
                return;
            }

            AnnotatedTypeMirror rightType = getAnnotatedType(rightExpr);
            // Check if the left side's value is known at compile time.
            AnnotatedTypeMirror valueTypeLeft =
                    valueAnnotatedTypeFactory.getAnnotatedType(leftExpr);
            Integer maybeValLeft = maybeValFromValueType(valueTypeLeft);
            if (maybeValLeft != null) {
                int val = maybeValLeft.intValue();
                addAnnotationForLiteralMultiply(val, rightType, type);
                return;
            }

            /* This section handles generic annotations:
             *   pos * pos becomes pos
             *   nn * pos becomes nn
             *   nn * nn becomes nn
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
            type.addAnnotation(UNKNOWN);
            return;
        }

        /**
         *  When the value on the left is known at compile time.
         */
        private void addAnnotationForLiteralDivideLeft(
                int val, AnnotatedTypeMirror rightType, AnnotatedTypeMirror type) {
            if (val == 0) {
                type.addAnnotation(NN);
                return;
            } else if (val == 1) {
                if (rightType.hasAnnotation(NN) || rightType.hasAnnotation(POS)) {
                    type.addAnnotation(NN);
                } else {
                    // (1 / x) can't be outside the range [-1, 1] when x is an integer.
                    type.addAnnotation(GTEN1);
                }
                return;
            }
        }

        /**
         *   When the value on the right is known at compile time.
         *   If the value is zero, then we've discovered division by zero.
         *   We over-approximate division by zero as positive infinity so that
         *   users aren't warned about dead code that's dividing by zero. We
         *   assume that actual code won't include literal divide by zeros...
         */
        private void addAnnotationForLiteralDivideRight(
                int val, AnnotatedTypeMirror leftType, AnnotatedTypeMirror type) {
            if (val == 0) {
                // If we get here then this is a divide by zero error. See above comment.
                type.addAnnotation(POS);
                return;
            } else if (val == 1) {
                type.addAnnotation(leftType.getAnnotationInHierarchy(POS));
                return;
            }
        }

        /**
         *  <pre>
         *  addAnnotationForDivide handles these cases:
         *	lit 0 / * &rarr; nn (=0)
         *      lit 1 / {pos, nn} &rarr; nn
         *      lit 1 / * &rarr; gten1
         *      * / lit 1 &rarr; *
         *      pos / {pos, nn} &rarr; nn (can round to zero)
         *      * / {pos, nn} &rarr; *
         *      * / * &rarr; lbu
         *  </pre>
         */
        public void addAnnotationForDivide(
                ExpressionTree leftExpr, ExpressionTree rightExpr, AnnotatedTypeMirror type) {

            AnnotatedTypeMirror leftType = getAnnotatedType(leftExpr);
            // Check if the right side's value is known at compile time.
            AnnotatedTypeMirror valueTypeRight =
                    valueAnnotatedTypeFactory.getAnnotatedType(rightExpr);
            Integer maybeValRight = maybeValFromValueType(valueTypeRight);
            if (maybeValRight != null) {
                int val = maybeValRight.intValue();
                addAnnotationForLiteralDivideRight(val, leftType, type);
            }

            AnnotatedTypeMirror rightType = getAnnotatedType(rightExpr);
            // Check if the left side's value is known at compile time.
            AnnotatedTypeMirror valueTypeLeft =
                    valueAnnotatedTypeFactory.getAnnotatedType(leftExpr);
            Integer maybeValLeft = maybeValFromValueType(valueTypeLeft);
            if (maybeValLeft != null) {
                int val = maybeValLeft.intValue();
                addAnnotationForLiteralDivideLeft(val, leftType, type);
            }

            /* This section handles generic annotations:
             *    pos / {pos, nn} -> nn (can round to zero)
             *    * / {pos, nn} -> *
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
            // We don't know anything about other stuff.
            type.addAnnotation(UNKNOWN);
            return;
        }

        /**
         *  When we take a remainder with 1 or -1 as the divisor, we know the answer ahead of time.
         */
        private void addAnnotationForLiteralRemainder(int val, AnnotatedTypeMirror type) {
            if (val == 1 || val == -1) {
                type.addAnnotation(NN);
                return;
            }
        }

        /**
         *  addAnnotationForRemainder handles these cases:
         *     * % 1/-1 becomes nn
         *     pos/nn % * becomes nn
         *     gten1 % * becomes gten1
         *     * % * becomes lbu
         */
        public void addAnnotationForRemainder(
                ExpressionTree leftExpr, ExpressionTree rightExpr, AnnotatedTypeMirror type) {
            AnnotatedTypeMirror leftType = getAnnotatedType(leftExpr);
            AnnotatedTypeMirror rightType = getAnnotatedType(rightExpr);

            // Check if the right side's value is known at compile time.
            AnnotatedTypeMirror valueTypeRight =
                    valueAnnotatedTypeFactory.getAnnotatedType(rightExpr);
            Integer maybeValRight = maybeValFromValueType(valueTypeRight);
            if (maybeValRight != null) {
                int val = maybeValRight.intValue();
                addAnnotationForLiteralRemainder(val, type);
            }

            /* This section handles generic annotations:
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

            // We don't know anything about other stuff.
            type.addAnnotation(UNKNOWN);
            return;
        }
    }
}
