package org.checkerframework.checker.index.lowerbound;

import static org.checkerframework.checker.index.IndexUtil.getExactValue;
import static org.checkerframework.checker.index.IndexUtil.getPossibleValues;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.UnaryTree;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import org.checkerframework.checker.index.IndexMethodIdentifier;
import org.checkerframework.checker.index.IndexUtil;
import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.IndexOrHigh;
import org.checkerframework.checker.index.qual.IndexOrLow;
import org.checkerframework.checker.index.qual.LengthOf;
import org.checkerframework.checker.index.qual.LowerBoundBottom;
import org.checkerframework.checker.index.qual.LowerBoundUnknown;
import org.checkerframework.checker.index.qual.NegativeIndexFor;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.PolyIndex;
import org.checkerframework.checker.index.qual.PolyLowerBound;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.index.searchindex.SearchIndexAnnotatedTypeFactory;
import org.checkerframework.checker.index.searchindex.SearchIndexChecker;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.common.value.qual.BottomVal;
import org.checkerframework.common.value.util.Range;
import org.checkerframework.framework.qual.PolyAll;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Implements the introduction rules for the Lower Bound Checker.
 *
 * <pre>
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
 *
 * In general, check whether the constant Value Checker can determine the value of a variable; if it
 * can, use that; if not, use more specific rules based on expression type. These rules are
 * documented on the functions implementing them.
 */
public class LowerBoundAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /** The canonical @{@link GTENegativeOne} annotation. */
    public final AnnotationMirror GTEN1 = AnnotationUtils.fromClass(elements, GTENegativeOne.class);
    /** The canonical @{@link NonNegative} annotation. */
    public final AnnotationMirror NN = AnnotationUtils.fromClass(elements, NonNegative.class);
    /** The canonical @{@link Positive} annotation. */
    public final AnnotationMirror POS = AnnotationUtils.fromClass(elements, Positive.class);
    /** The bottom annotation. */
    public final AnnotationMirror BOTTOM =
            AnnotationUtils.fromClass(elements, LowerBoundBottom.class);
    /** The canonical @{@link LowerBoundUnknown} annotation. */
    public final AnnotationMirror UNKNOWN =
            AnnotationUtils.fromClass(elements, LowerBoundUnknown.class);
    /** The canonical @{@link PolyLowerBound} annotation. */
    public final AnnotationMirror POLY = AnnotationUtils.fromClass(elements, PolyLowerBound.class);

    private final IndexMethodIdentifier imf;

    public LowerBoundAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        addAliasedAnnotation(IndexFor.class, NN);
        addAliasedAnnotation(IndexOrLow.class, GTEN1);
        addAliasedAnnotation(IndexOrHigh.class, NN);
        addAliasedAnnotation(LengthOf.class, NN);
        addAliasedAnnotation(PolyAll.class, POLY);
        addAliasedAnnotation(PolyIndex.class, POLY);

        imf = new IndexMethodIdentifier(processingEnv);

        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        // Because the Index Checker is a subclass, the qualifiers have to be explicitly defined.
        return new LinkedHashSet<>(
                Arrays.asList(
                        Positive.class,
                        NonNegative.class,
                        GTENegativeOne.class,
                        LowerBoundUnknown.class,
                        PolyLowerBound.class,
                        LowerBoundBottom.class));
    }

    /**
     * Takes a value type (only interesting if it's an IntVal), and converts it to a lower bound
     * type. If the new lower bound type is more specific than type, convert type to that type.
     *
     * @param valueType the Value Checker type
     * @param type the current lower bound type of the expression being evaluated
     */
    private void addLowerBoundTypeFromValueType(
            AnnotatedTypeMirror valueType, AnnotatedTypeMirror type) {
        AnnotationMirror anm = getLowerBoundAnnotationFromValueType(valueType);
        if (!type.isAnnotatedInHierarchy(UNKNOWN)) {
            if (!AnnotationUtils.areSameByClass(anm, LowerBoundUnknown.class)) {
                type.addAnnotation(anm);
            }
            return;
        }
        if (qualHierarchy.isSubtype(anm, type.getAnnotationInHierarchy(UNKNOWN))) {
            type.replaceAnnotation(anm);
        }
    }

    @Override
    public void addComputedTypeAnnotations(Element element, AnnotatedTypeMirror type) {
        super.addComputedTypeAnnotations(element, type);
        if (element != null) {
            AnnotatedTypeMirror valueType =
                    getValueAnnotatedTypeFactory().getAnnotatedType(element);
            addLowerBoundTypeFromValueType(valueType, type);
        }
    }

    @Override
    public void addComputedTypeAnnotations(Tree tree, AnnotatedTypeMirror type, boolean iUseFlow) {
        super.addComputedTypeAnnotations(tree, type, iUseFlow);
        // If dataflow shouldn't be used to compute this type, then do not use the result from
        // the Value Checker, because dataflow is used to compute that type.  (Without this,
        // "int i = 1; --i;" fails.)
        if (iUseFlow && tree != null && TreeUtils.isExpressionTree(tree)) {
            AnnotatedTypeMirror valueType = getValueAnnotatedTypeFactory().getAnnotatedType(tree);
            addLowerBoundTypeFromValueType(valueType, type);
        }
    }

    /** Returns the Value Checker's annotated type factory. */
    public ValueAnnotatedTypeFactory getValueAnnotatedTypeFactory() {
        return getTypeFactoryOfSubchecker(ValueChecker.class);
    }

    /** Returns the SearchIndexFor Checker's annotated type factory. */
    public SearchIndexAnnotatedTypeFactory getSearchIndexAnnotatedTypeFactory() {
        return getTypeFactoryOfSubchecker(SearchIndexChecker.class);
    }

    /** Returns the type in the lower bound hierarchy that a Value Checker type corresponds to. */
    private AnnotationMirror getLowerBoundAnnotationFromValueType(AnnotatedTypeMirror valueType) {
        Range possibleValues = getPossibleValues(valueType, getValueAnnotatedTypeFactory());
        // possibleValues is null if the Value Checker does not have any estimate.
        if (possibleValues == null) {
            // possibleValues is null if there is no IntVal annotation on the type - such as
            // when there is a BottomVal annotation. In that case, give this the LBC's bottom type.
            if (AnnotationUtils.containsSameByClass(valueType.getAnnotations(), BottomVal.class)) {
                return BOTTOM;
            }
            return UNKNOWN;
        }
        // The annotation of the whole list is the min of the list.
        long lvalMin = possibleValues.from;
        // Turn it into an integer.
        int valMin = (int) Math.max(Math.min(Integer.MAX_VALUE, lvalMin), Integer.MIN_VALUE);
        return anmFromVal(valMin);
    }

    /** Determine the annotation that should be associated with a literal. */
    private AnnotationMirror anmFromVal(long val) {
        if (val >= 1) {
            return POS;
        } else if (val >= 0) {
            return NN;
        } else if (val >= -1) {
            return GTEN1;
        } else {
            return UNKNOWN;
        }
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                new LowerBoundTreeAnnotator(this), super.createTreeAnnotator());
    }

    private class LowerBoundTreeAnnotator extends TreeAnnotator {
        public LowerBoundTreeAnnotator(AnnotatedTypeFactory annotatedTypeFactory) {
            super(annotatedTypeFactory);
        }

        /**
         * Sets typeDst to the immediate supertype of typeSrc, unless typeSrc is already Positive.
         * Implements the following transitions:
         *
         * <pre>
         *      pos &rarr; pos
         *      nn &rarr; pos
         *      gte-1 &rarr; nn
         *      lbu &rarr; lbu
         *  </pre>
         */
        private void promoteType(AnnotatedTypeMirror typeSrc, AnnotatedTypeMirror typeDst) {
            if (typeSrc.hasAnnotation(POS)) {
                typeDst.replaceAnnotation(POS);
            } else if (typeSrc.hasAnnotation(NN)) {
                typeDst.replaceAnnotation(POS);
            } else if (typeSrc.hasAnnotation(GTEN1)) {
                typeDst.replaceAnnotation(NN);
            } else { //Only unknown is left.
                typeDst.replaceAnnotation(UNKNOWN);
            }
        }

        /**
         * Sets typeDst to the immediate subtype of typeSrc, unless typeSrc is already
         * LowerBoundUnknown. Implements the following transitions:
         *
         * <pre>
         *       pos &rarr; nn
         *       nn &rarr; gte-1
         *       gte-1, lbu &rarr; lbu
         *  </pre>
         */
        private void demoteType(AnnotatedTypeMirror typeSrc, AnnotatedTypeMirror typeDst) {
            if (typeSrc.hasAnnotation(POS)) {
                typeDst.replaceAnnotation(NN);
            } else if (typeSrc.hasAnnotation(NN)) {
                typeDst.replaceAnnotation(GTEN1);
            } else { // GTEN1 and UNKNOWN both become UNKNOWN.
                typeDst.replaceAnnotation(UNKNOWN);
            }
        }

        /** Call increment and decrement helper functions. */
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
                case POSTFIX_INCREMENT:
                case POSTFIX_DECREMENT:
                    // Do nothing. The CF should take care of these itself.
                    break;
                case BITWISE_COMPLEMENT:
                    handleBitWiseComplement(
                            getSearchIndexAnnotatedTypeFactory()
                                    .getAnnotatedType(tree.getExpression()),
                            typeDst);
                    break;
                default:
                    break;
            }
            return super.visitUnary(tree, typeDst);
        }

        /**
         * Bitwise complement converts between {@code @NegativeIndexFor} and {@code @IndexOrHigh}.
         * This handles the lowerbound part of that type, so the result is converted to
         * {@code @NonNegative}.
         *
         * @param searchIndexType the type of an expression in a bitwise complement. For instance,
         *     in {@code ~x}, this is the type of {@code x}.
         * @param typeDst the type of the entire bitwise complement expression. It is modified by
         *     this method.
         */
        private void handleBitWiseComplement(
                AnnotatedTypeMirror searchIndexType, AnnotatedTypeMirror typeDst) {
            if (AnnotationUtils.containsSameByClass(
                    searchIndexType.getAnnotations(), NegativeIndexFor.class)) {
                typeDst.addAnnotation(NN);
            }
        }

        /** Special handling for Math.max. The return is the GLB of the arguments. */
        @Override
        public Void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
            if (imf.isMathMax(tree, processingEnv)) {
                ExpressionTree left = tree.getArguments().get(0);
                ExpressionTree right = tree.getArguments().get(1);
                type.replaceAnnotation(
                        qualHierarchy.greatestLowerBound(
                                getAnnotatedType(left).getAnnotationInHierarchy(POS),
                                getAnnotatedType(right).getAnnotationInHierarchy(POS)));
            }
            return super.visitMethodInvocation(tree, type);
        }

        /**
         * Looks up the minlen of a member select tree. Returns null if the tree doesn't represent
         * an array's length field.
         */
        private Integer getMinLenFromMemberSelectTree(MemberSelectTree tree) {
            if (TreeUtils.isArrayLengthAccess(tree)) {
                return IndexUtil.getMinLenFromTree(tree, getValueAnnotatedTypeFactory());
            }
            return null;
        }

        /**
         * Looks up the minlen of a method invocation tree. Returns null if the tree doesn't
         * represent an string length method.
         */
        private Integer getMinLenFromMethodInvocationTree(MethodInvocationTree tree) {
            if (imf.isStringLength(tree, processingEnv)) {
                return IndexUtil.getMinLenFromTree(tree, getValueAnnotatedTypeFactory());
            }
            return null;
        }

        /**
         * For dealing with array length expressions. Looks for array length accesses specifically,
         * then dispatches to the MinLen checker to determine the length of the relevant array. If
         * it's found, use it to give the expression a type.
         */
        @Override
        public Void visitMemberSelect(MemberSelectTree tree, AnnotatedTypeMirror type) {
            Integer minLen = getMinLenFromMemberSelectTree(tree);
            if (minLen != null) {
                type.replaceAnnotation(anmFromVal(minLen));
            }
            return super.visitMemberSelect(tree, type);
        }

        /**
         * Dispatch to binary operator helper methods. The Lower Bound Checker currently handles
         * addition, subtraction, multiplication, division, and remainder.
         */
        @Override
        public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
            // Check if this is a string concatenation. If so, bail.
            if (TreeUtils.isStringConcatenation(tree)) {
                type.addAnnotation(UNKNOWN);
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
                case AND:
                    addAnnotationForAnd(left, right, type);
                    break;
                case RIGHT_SHIFT:
                    addAnnotationForRightShift(left, right, type);
                    break;
                default:
                    break;
            }
            return super.visitBinary(tree, type);
        }

        /**
         * Helper method for addAnnotationForPlus. Handles addition of constants.
         *
         * @param val the integer value of the constant
         * @param nonLiteralType the type of the side of the expression that isn't a constant
         * @param type the type of the result expression
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
                // This gets the type of nonLiteralType in our hierarchy (in which POS is the
                // bottom type).
                type.addAnnotation(nonLiteralType.getAnnotationInHierarchy(POS));
                return;
            } else if (val == 1) {
                promoteType(nonLiteralType, type);
                return;
            } else if (val >= 2) {
                if (!nonLiteralType.hasAnnotation(UNKNOWN)) {
                    // 2 + a positive, or a non-negative, or a non-negative-1 is a positive
                    type.addAnnotation(POS);
                    return;
                }
            }
            type.addAnnotation(UNKNOWN);
        }

        /**
         * addAnnotationForPlus handles the following cases:
         *
         * <pre>
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
        private void addAnnotationForPlus(
                ExpressionTree leftExpr, ExpressionTree rightExpr, AnnotatedTypeMirror type) {

            // Adding two literals is handled by visitBinary, so that
            // case can be ignored.
            AnnotatedTypeMirror leftType = getAnnotatedType(leftExpr);
            // Check if the right side's value is known at compile time.

            Long valRight = getExactValue(rightExpr, getValueAnnotatedTypeFactory());
            if (valRight != null) {
                addAnnotationForLiteralPlus(valRight.intValue(), leftType, type);
                return;
            }

            AnnotatedTypeMirror rightType = getAnnotatedType(rightExpr);
            // Check if the left side's value is known at compile time.

            Long valLeft = getExactValue(leftExpr, getValueAnnotatedTypeFactory());
            if (valLeft != null) {
                addAnnotationForLiteralPlus(valLeft.intValue(), rightType, type);
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

            // * + * -> lbu.
            type.addAnnotation(UNKNOWN);
        }

        /**
         * addAnnotationForMinus handles the following cases:
         *
         * <pre>
         *      * - lit &rarr; call plus(*, -1 * the value of the lit)
         *      * - * &rarr; lbu
         *  </pre>
         */
        private void addAnnotationForMinus(
                ExpressionTree leftExpr, ExpressionTree rightExpr, AnnotatedTypeMirror type) {

            // Check if the right side's value is known at compile time.
            Long valRight = getExactValue(rightExpr, getValueAnnotatedTypeFactory());
            if (valRight != null) {
                AnnotatedTypeMirror leftType = getAnnotatedType(leftExpr);
                // Instead of a separate method for subtraction, add the negative of a constant.
                addAnnotationForLiteralPlus(-1 * valRight.intValue(), leftType, type);

                Integer minLen = null;
                // Check if the left side is a field access of an array's length,
                // or invocation of String.length. If so,
                // try to look up the MinLen of the array, and potentially keep
                // this either NN or POS instead of GTEN1 or LBU.
                if (leftExpr.getKind() == Kind.MEMBER_SELECT) {
                    MemberSelectTree mstree = (MemberSelectTree) leftExpr;
                    minLen = getMinLenFromMemberSelectTree(mstree);
                } else if (leftExpr.getKind() == Kind.METHOD_INVOCATION) {
                    MethodInvocationTree mitree = (MethodInvocationTree) leftExpr;
                    minLen = getMinLenFromMethodInvocationTree(mitree);
                }

                if (minLen != null) {
                    type.replaceAnnotation(anmFromVal(minLen - valRight));
                }

                return;
            }

            // The checker can't reason about arbitrary (i.e. non-literal)
            // things that are being subtracted, so it gives up.
            type.addAnnotation(UNKNOWN);
        }

        /**
         * Helper function for addAnnotationForMultiply. Handles compile-time known constants.
         *
         * @param val the integer value of the constant
         * @param nonLiteralType the type of the side of the expression that isn't a constant
         * @param type the type of the result expression
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

        private boolean checkForMathRandomSpecialCase(
                ExpressionTree randTree, ExpressionTree arrLenTree, AnnotatedTypeMirror type) {
            if (randTree.getKind() == Kind.METHOD_INVOCATION
                    && TreeUtils.isArrayLengthAccess(arrLenTree)) {
                MethodInvocationTree miTree = (MethodInvocationTree) randTree;

                if (imf.isMathRandom(miTree, processingEnv)) {
                    // This is Math.random() * array.length, which must be NonNegative
                    type.addAnnotation(NN);
                    return true;
                }

                if (imf.isRandomNextDouble(miTree, processingEnv)) {
                    // This is Random.nextDouble() * array.length, which must be NonNegative
                    type.addAnnotation(NN);
                    return true;
                }
            }
            return false;
        }

        /**
         * addAnnotationForMultiply handles the following cases:
         *
         * <pre>
         *        * * lit 0 &rarr; nn (=0)
         *        * * lit 1 &rarr; *
         *        pos * pos &rarr; pos
         *        pos * nn &rarr; nn
         *        nn * nn &rarr; nn
         *        * * * &rarr; lbu
         *  </pre>
         */
        private void addAnnotationForMultiply(
                ExpressionTree leftExpr, ExpressionTree rightExpr, AnnotatedTypeMirror type) {

            // Special handling for multiplying an array length by a Math.random().
            if (checkForMathRandomSpecialCase(rightExpr, leftExpr, type)
                    || checkForMathRandomSpecialCase(leftExpr, rightExpr, type)) {
                return;
            }

            AnnotatedTypeMirror leftType = getAnnotatedType(leftExpr);
            // Check if the right side's value is known at compile time.

            Long valRight = getExactValue(rightExpr, getValueAnnotatedTypeFactory());
            if (valRight != null) {
                addAnnotationForLiteralMultiply(valRight.intValue(), leftType, type);
                return;
            }

            AnnotatedTypeMirror rightType = getAnnotatedType(rightExpr);
            // Check if the left side's value is known at compile time.
            Long valLeft = getExactValue(leftExpr, getValueAnnotatedTypeFactory());
            if (valLeft != null) {
                addAnnotationForLiteralMultiply(valLeft.intValue(), rightType, type);
                return;
            }

            /* This section handles generic annotations:
             *   pos * pos -> pos
             *   nn * pos -> nn
             *   nn * nn -> nn
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
        }

        /** When the value on the left is known at compile time. */
        private void addAnnotationForLiteralDivideLeft(
                int val, AnnotatedTypeMirror rightType, AnnotatedTypeMirror type) {
            if (val == 0) {
                type.addAnnotation(NN);
            } else if (val == 1) {
                if (rightType.hasAnnotation(NN) || rightType.hasAnnotation(POS)) {
                    type.addAnnotation(NN);
                } else {
                    // (1 / x) can't be outside the range [-1, 1] when x is an integer.
                    type.addAnnotation(GTEN1);
                }
            }
        }

        /** When the value on the right is known at compile time. */
        private void addAnnotationForLiteralDivideRight(
                int val, AnnotatedTypeMirror leftType, AnnotatedTypeMirror type) {
            if (val == 0) {
                // Reaching this indicates a divide by zero error. If the value is zero, then this is
                // division by zero. Division by zero is treated as bottom so that users
                // aren't warned about dead code that's dividing by zero. This code assumes that non-dead
                // code won't include literal divide by zeros...
                type.addAnnotation(BOTTOM);
            } else if (val == 1) {
                type.addAnnotation(leftType.getAnnotationInHierarchy(POS));
            } else if (val >= 2) {
                if (leftType.hasAnnotation(NonNegative.class)
                        || leftType.hasAnnotation(Positive.class)) {
                    type.addAnnotation(NN);
                }
            }
        }

        /**
         * addAnnotationForDivide handles these cases:
         *
         * <pre>
         * lit 0 / * &rarr; nn (=0)
         *      * / lit 0 &rarr; pos
         *      lit 1 / {pos, nn} &rarr; nn
         *      lit 1 / * &rarr; gten1
         *      * / lit 1 &rarr; *
         *      {pos, nn} / lit &gt;1 &rarr; nn
         *      pos / {pos, nn} &rarr; nn (can round to zero)
         *      * / {pos, nn} &rarr; *
         *      * / * &rarr; lbu
         *  </pre>
         */
        private void addAnnotationForDivide(
                ExpressionTree leftExpr, ExpressionTree rightExpr, AnnotatedTypeMirror type) {

            AnnotatedTypeMirror leftType = getAnnotatedType(leftExpr);
            // Check if the right side's value is known at compile time.

            Long valRight = getExactValue(rightExpr, getValueAnnotatedTypeFactory());
            if (valRight != null) {
                addAnnotationForLiteralDivideRight(valRight.intValue(), leftType, type);
                return;
            }

            AnnotatedTypeMirror rightType = getAnnotatedType(rightExpr);
            // Check if the left side's value is known at compile time.
            Long valLeft = getExactValue(leftExpr, getValueAnnotatedTypeFactory());
            if (valLeft != null) {
                addAnnotationForLiteralDivideLeft(valLeft.intValue(), leftType, type);
                return;
            }

            /* This section handles generic annotations:
             *    pos / {pos, nn} -> nn (can round to zero)
             *    * / {pos, nn} -> *
             */
            if (leftType.hasAnnotation(POS)
                    && (rightType.hasAnnotation(POS) || rightType.hasAnnotation(NN))) {
                type.addAnnotation(NN);
                return;
            }
            if (rightType.hasAnnotation(POS) || rightType.hasAnnotation(NN)) {
                type.addAnnotation(leftType.getAnnotationInHierarchy(POS));
                return;
            }
            // Everything else is unknown.
            type.addAnnotation(UNKNOWN);
        }

        /** A remainder with 1 or -1 as the divisor always results in zero. */
        private void addAnnotationForLiteralRemainder(int val, AnnotatedTypeMirror type) {
            if (val == 1 || val == -1) {
                type.addAnnotation(NN);
            }
        }

        /**
         * addAnnotationForRemainder handles these cases: * % 1/-1 &rarr; nn pos/nn % * &rarr; nn
         * gten1 % * &rarr; gten1 * % * &rarr; lbu
         */
        public void addAnnotationForRemainder(
                ExpressionTree leftExpr, ExpressionTree rightExpr, AnnotatedTypeMirror type) {
            AnnotatedTypeMirror leftType = getAnnotatedType(leftExpr);
            AnnotatedTypeMirror rightType = getAnnotatedType(rightExpr);

            // Check if the right side's value is known at compile time.
            Long valRight = getExactValue(rightExpr, getValueAnnotatedTypeFactory());
            if (valRight != null) {
                addAnnotationForLiteralRemainder(valRight.intValue(), type);
            }

            /* This section handles generic annotations:
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

            // Everything else is unknown.
            type.addAnnotation(UNKNOWN);
        }
    }

    /** Handles shifts. * &gt;&gt; NonNegative &rarr; NonNegative */
    private void addAnnotationForRightShift(
            ExpressionTree leftExpr, ExpressionTree rightExpr, AnnotatedTypeMirror type) {
        AnnotatedTypeMirror rightType = getAnnotatedType(rightExpr);
        AnnotatedTypeMirror leftType = getAnnotatedType(leftExpr);
        if (leftType.hasAnnotation(NN) || leftType.hasAnnotation(POS)) {
            if (rightType.hasAnnotation(NN) || rightType.hasAnnotation(POS)) {
                type.addAnnotation(NN);
                return;
            }
        }
        type.addAnnotation(UNKNOWN);
    }

    /**
     * Handles masking. Particularly, handles the following cases: * &amp; NonNegative &rarr;
     * NonNegative
     */
    private void addAnnotationForAnd(
            ExpressionTree leftExpr, ExpressionTree rightExpr, AnnotatedTypeMirror type) {
        AnnotatedTypeMirror rightType = getAnnotatedType(rightExpr);
        if (rightType.hasAnnotation(NN) || rightType.hasAnnotation(POS)) {
            type.addAnnotation(NN);
            return;
        }

        AnnotatedTypeMirror leftType = getAnnotatedType(leftExpr);
        if (leftType.hasAnnotation(NN) || leftType.hasAnnotation(POS)) {
            type.addAnnotation(NN);
            return;
        }

        type.addAnnotation(UNKNOWN);
    }
}
