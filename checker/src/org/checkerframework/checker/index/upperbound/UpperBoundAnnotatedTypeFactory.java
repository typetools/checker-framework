package org.checkerframework.checker.index.upperbound;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.TreePath;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.index.IndexMethodIdentifier;
import org.checkerframework.checker.index.IndexUtil;
import org.checkerframework.checker.index.lowerbound.LowerBoundAnnotatedTypeFactory;
import org.checkerframework.checker.index.lowerbound.LowerBoundChecker;
import org.checkerframework.checker.index.minlen.MinLenAnnotatedTypeFactory;
import org.checkerframework.checker.index.minlen.MinLenChecker;
import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.IndexOrHigh;
import org.checkerframework.checker.index.qual.IndexOrLow;
import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.checker.index.qual.LTOMLengthOf;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.PolyIndex;
import org.checkerframework.checker.index.qual.PolyUpperBound;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.index.qual.SameLen;
import org.checkerframework.checker.index.qual.UpperBoundBottom;
import org.checkerframework.checker.index.qual.UpperBoundUnknown;
import org.checkerframework.checker.index.samelen.SameLenAnnotatedTypeFactory;
import org.checkerframework.checker.index.samelen.SameLenChecker;
import org.checkerframework.checker.index.upperbound.UBQualifier.LessThanLengthOf;
import org.checkerframework.checker.index.upperbound.UBQualifier.UpperBoundUnknownQualifier;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.qual.PolyAll;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.framework.util.FlowExpressionParseUtil;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionContext;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.framework.util.dependenttypes.DependentTypesError;
import org.checkerframework.framework.util.dependenttypes.DependentTypesHelper;
import org.checkerframework.framework.util.dependenttypes.DependentTypesTreeAnnotator;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Implements the introduction rules for the Upper Bound Checker. Works primarily by way of querying
 * the MinLen Checker and comparing the min lengths of arrays to the known values of variables as
 * supplied by the Value Checker.
 */
public class UpperBoundAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    public final AnnotationMirror UNKNOWN, BOTTOM, POLY;

    private final IndexMethodIdentifier imf;

    public UpperBoundAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        UNKNOWN = AnnotationUtils.fromClass(elements, UpperBoundUnknown.class);
        BOTTOM = AnnotationUtils.fromClass(elements, UpperBoundBottom.class);
        POLY = AnnotationUtils.fromClass(elements, PolyUpperBound.class);

        addAliasedAnnotation(IndexFor.class, createLTLengthOfAnnotation());
        addAliasedAnnotation(IndexOrLow.class, createLTLengthOfAnnotation());
        addAliasedAnnotation(IndexOrHigh.class, createLTEqLengthOfAnnotation());
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
                        UpperBoundUnknown.class,
                        LTEqLengthOf.class,
                        LTLengthOf.class,
                        LTOMLengthOf.class,
                        UpperBoundBottom.class,
                        PolyUpperBound.class));
    }

    /**
     * Provides a way to query the Constant Value Checker, which computes the values of expressions
     * known at compile time (constant prop + folding).
     */
    ValueAnnotatedTypeFactory getValueAnnotatedTypeFactory() {
        return getTypeFactoryOfSubchecker(ValueChecker.class);
    }

    /**
     * Provides a way to query the Min Len (minimum length) Checker, which computes the lengths of
     * arrays.
     */
    MinLenAnnotatedTypeFactory getMinLenAnnotatedTypeFactory() {
        return getTypeFactoryOfSubchecker(MinLenChecker.class);
    }

    /**
     * Provides a way to query the SameLen (same length) Checker, which determines the relationships
     * among the lengths of arrays.
     */
    SameLenAnnotatedTypeFactory getSameLenAnnotatedTypeFactory() {
        return getTypeFactoryOfSubchecker(SameLenChecker.class);
    }

    /**
     * Provides a way to query the Lower Bound Checker, which determines whether each integer in the
     * program is non-negative or not, and checks that no possibly negative integers are used to
     * access arrays.
     */
    private LowerBoundAnnotatedTypeFactory getLowerBoundAnnotatedTypeFactory() {
        return getTypeFactoryOfSubchecker(LowerBoundChecker.class);
    }

    @Override
    protected DependentTypesHelper createDependentTypesHelper() {
        return new DependentTypesHelper(this) {
            @Override
            protected String standardizeString(
                    final String expression,
                    FlowExpressionContext context,
                    TreePath localScope,
                    boolean useLocalScope) {
                if (DependentTypesError.isExpressionError(expression)) {
                    return expression;
                }
                if (indexOf(expression, '-', '+', 0) == -1) {
                    return super.standardizeString(expression, context, localScope, useLocalScope);
                }

                OffsetEquation equation = OffsetEquation.createOffsetFromJavaExpression(expression);
                if (equation.hasError()) {
                    return equation.getError();
                }
                try {
                    equation.standardizeAndViewpointAdaptExpressions(
                            context, localScope, useLocalScope);
                } catch (FlowExpressionParseUtil.FlowExpressionParseException e) {
                    return new DependentTypesError(expression, e).toString();
                }

                return equation.toString();
            }

            private int indexOf(String expression, char a, char b, int index) {
                int aIndex = expression.indexOf(a, index);
                int bIndex = expression.indexOf(b, index);
                if (aIndex == -1) {
                    return bIndex;
                } else if (bIndex == -1) {
                    return aIndex;
                } else {
                    return Math.min(aIndex, bIndex);
                }
            }

            @Override
            public TreeAnnotator createDependentTypesTreeAnnotator(AnnotatedTypeFactory factory) {
                return new DependentTypesTreeAnnotator(factory, this) {
                    @Override
                    public Void visitMemberSelect(MemberSelectTree tree, AnnotatedTypeMirror type) {
                        // UpperBoundTreeAnnotator changes the type of array.length to @LTEL
                        // ("array"). If the DependentTypesTreeAnnotator tries to viewpoint
                        // adapt it based on the declaration of length; it will fail.
                        if (TreeUtils.isArrayLengthAccess(tree)) {
                            return null;
                        }
                        return super.visitMemberSelect(tree, type);
                    }
                };
            }
        };
    }

    @Override
    public AnnotationMirror aliasedAnnotation(AnnotationMirror a) {
        if (AnnotationUtils.areSameByClass(a, IndexFor.class)) {
            List<String> stringList =
                    AnnotationUtils.getElementValueArray(a, "value", String.class, true);
            return createLTLengthOfAnnotation(stringList.toArray(new String[0]));
        }
        if (AnnotationUtils.areSameByClass(a, IndexOrLow.class)) {
            List<String> stringList =
                    AnnotationUtils.getElementValueArray(a, "value", String.class, true);
            return createLTLengthOfAnnotation(stringList.toArray(new String[0]));
        }
        if (AnnotationUtils.areSameByClass(a, IndexOrHigh.class)) {
            List<String> stringList =
                    AnnotationUtils.getElementValueArray(a, "value", String.class, true);
            return createLTEqLengthOfAnnotation(stringList.toArray(new String[0]));
        }
        return super.aliasedAnnotation(a);
    }

    /**
     * Queries the SameLen Checker to return the type that the SameLen Checker associates with the
     * given expression tree.
     */
    public AnnotationMirror sameLenAnnotationFromExpressionTree(ExpressionTree tree) {
        AnnotatedTypeMirror sameLenType = getSameLenAnnotatedTypeFactory().getAnnotatedType(tree);
        return sameLenType.getAnnotation(SameLen.class);
    }

    // Wrapper methods for accessing the IndexMethodIdentifier.

    public boolean isMathMin(Tree methodTree) {
        return imf.isMathMin(methodTree, processingEnv);
    }

    public boolean isRandomNextInt(Tree methodTree) {
        return imf.isRandomNextInt(methodTree, processingEnv);
    }

    AnnotationMirror createLTLengthOfAnnotation(String... names) {
        AnnotationBuilder builder = new AnnotationBuilder(getProcessingEnv(), LTLengthOf.class);
        if (names == null) {
            names = new String[0];
        }
        builder.setValue("value", names);
        return builder.build();
    }

    AnnotationMirror createLTEqLengthOfAnnotation(String... names) {
        AnnotationBuilder builder = new AnnotationBuilder(getProcessingEnv(), LTEqLengthOf.class);
        if (names == null) {
            names = new String[0];
        }
        builder.setValue("value", names);
        return builder.build();
    }

    /**
     * Returns true iff the given node has the passed Lower Bound qualifier according to the LBC.
     * The last argument should be Positive.class, NonNegative.class, or GTENegativeOne.class.
     */
    public boolean hasLowerBoundTypeByClass(Node node, Class<? extends Annotation> classOfType) {
        return AnnotationUtils.areSameByClass(
                getLowerBoundAnnotatedTypeFactory()
                        .getAnnotatedType(node.getTree())
                        .getAnnotationInHierarchy(getLowerBoundAnnotatedTypeFactory().UNKNOWN),
                classOfType);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new UpperBoundQualifierHierarchy(factory);
    }

    /**
     * The qualifier hierarchy for the upperbound type system. The qh is responsible for determining
     * the relationships within the qualifiers - especially subtyping relations.
     */
    protected final class UpperBoundQualifierHierarchy extends MultiGraphQualifierHierarchy {
        /** @param factory MultiGraphFactory to use to construct this */
        public UpperBoundQualifierHierarchy(
                MultiGraphQualifierHierarchy.MultiGraphFactory factory) {
            super(factory);
        }

        @Override
        public AnnotationMirror greatestLowerBound(AnnotationMirror a1, AnnotationMirror a2) {
            UBQualifier a1Obj = UBQualifier.createUBQualifier(a1);
            UBQualifier a2Obj = UBQualifier.createUBQualifier(a2);
            UBQualifier glb = a1Obj.glb(a2Obj);
            return convertUBQualifierToAnnotation(glb);
        }

        /**
         * Determines the least upper bound of a1 and a2. If a1 and a2 are both the same type of
         * Value annotation, then the LUB is the result of taking the intersection of values from
         * both a1 and a2.
         *
         * @return the least upper bound of a1 and a2
         */
        @Override
        public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
            UBQualifier a1Obj = UBQualifier.createUBQualifier(a1);
            UBQualifier a2Obj = UBQualifier.createUBQualifier(a2);
            UBQualifier lub = a1Obj.lub(a2Obj);
            return convertUBQualifierToAnnotation(lub);
        }

        @Override
        public AnnotationMirror widenUpperBound(AnnotationMirror a, AnnotationMirror b) {
            UBQualifier a1Obj = UBQualifier.createUBQualifier(a);
            UBQualifier a2Obj = UBQualifier.createUBQualifier(b);
            UBQualifier lub = a1Obj.widenUpperBound(a2Obj);
            return convertUBQualifierToAnnotation(lub);
        }

        @Override
        public boolean implementsWidening() {
            return true;
        }

        /**
         * Computes subtyping as per the subtyping in the qualifier hierarchy structure unless both
         * annotations are the same. In this case, rhs is a subtype of lhs iff rhs contains at least
         * every element of lhs.
         *
         * @return true if rhs is a subtype of lhs, false otherwise
         */
        @Override
        public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {
            UBQualifier subtype = UBQualifier.createUBQualifier(subAnno);
            UBQualifier supertype = UBQualifier.createUBQualifier(superAnno);
            return subtype.isSubtype(supertype);
        }
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                new UpperBoundTreeAnnotator(this), super.createTreeAnnotator());
    }

    protected class UpperBoundTreeAnnotator extends TreeAnnotator {

        public UpperBoundTreeAnnotator(UpperBoundAnnotatedTypeFactory factory) {
            super(factory);
        }

        /**
         * This exists for Math.min and Random.nextInt, which must be special-cased.
         *
         * <ul>
         *   <li>Math.min has unusual semantics that combines annotations for the UBC.
         *   <li>The return type of Random.nextInt depends on the argument, but is not equal to it,
         *       so a polymorhpic qualifier is insufficient.
         * </ul>
         *
         * Other methods should not be special-cased here unless there is a compelling reason to do
         * so.
         */
        @Override
        public Void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
            if (isMathMin(tree)) {
                AnnotatedTypeMirror leftType = getAnnotatedType(tree.getArguments().get(0));
                AnnotatedTypeMirror rightType = getAnnotatedType(tree.getArguments().get(1));

                type.replaceAnnotation(
                        qualHierarchy.greatestLowerBound(
                                leftType.getAnnotationInHierarchy(UNKNOWN),
                                rightType.getAnnotationInHierarchy(UNKNOWN)));
            }
            if (isRandomNextInt(tree)) {
                AnnotatedTypeMirror argType = getAnnotatedType(tree.getArguments().get(0));
                AnnotationMirror anno = argType.getAnnotationInHierarchy(UNKNOWN);
                UBQualifier qualifier = UBQualifier.createUBQualifier(anno);
                qualifier = qualifier.plusOffset(1);
                type.replaceAnnotation(convertUBQualifierToAnnotation(qualifier));
            }
            return super.visitMethodInvocation(tree, type);
        }

        @Override
        public Void visitUnary(UnaryTree node, AnnotatedTypeMirror type) {
            // Dataflow refines this type if possible
            type.addAnnotation(UNKNOWN);
            return super.visitUnary(node, type);
        }

        @Override
        public Void visitCompoundAssignment(CompoundAssignmentTree node, AnnotatedTypeMirror type) {
            // Dataflow refines this type if possible
            type.addAnnotation(UNKNOWN);
            return super.visitCompoundAssignment(node, type);
        }

        @Override
        public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
            // A few small rules for addition/subtraction by 0/1, etc.
            if (TreeUtils.isStringConcatenation(tree)) {
                type.addAnnotation(UNKNOWN);
                return super.visitBinary(tree, type);
            }

            ExpressionTree left = tree.getLeftOperand();
            ExpressionTree right = tree.getRightOperand();
            switch (tree.getKind()) {
                case PLUS:
                case MINUS:
                    // Dataflow refines this type if possible
                    type.addAnnotation(UNKNOWN);
                    break;
                case MULTIPLY:
                    addAnnotationForMultiply(left, right, type);
                    break;
                case DIVIDE:
                    addAnnotationForDivide(left, right, type);
                    break;
                case AND:
                    addAnnotationForAnd(left, right, type);
                    break;
                default:
                    break;
            }
            return super.visitBinary(tree, type);
        }

        private void addAnnotationForAnd(
                ExpressionTree left, ExpressionTree right, AnnotatedTypeMirror type) {
            AnnotatedTypeMirror leftType = getAnnotatedType(left);
            AnnotatedTypeMirror leftLBType =
                    getLowerBoundAnnotatedTypeFactory().getAnnotatedType(left);
            AnnotationMirror leftResultType = UNKNOWN;
            if (leftLBType.hasAnnotation(NonNegative.class)
                    || leftLBType.hasAnnotation(Positive.class)) {
                leftResultType = leftType.getAnnotationInHierarchy(UNKNOWN);
            }

            AnnotatedTypeMirror rightType = getAnnotatedType(right);
            AnnotatedTypeMirror rightLBType =
                    getLowerBoundAnnotatedTypeFactory().getAnnotatedType(right);
            AnnotationMirror rightResultType = UNKNOWN;
            if (rightLBType.hasAnnotation(NonNegative.class)
                    || rightLBType.hasAnnotation(Positive.class)) {
                rightResultType = rightType.getAnnotationInHierarchy(UNKNOWN);
            }

            type.addAnnotation(qualHierarchy.greatestLowerBound(leftResultType, rightResultType));
        }

        private void addAnnotationForDivide(
                ExpressionTree numeratorTree,
                ExpressionTree divisorTree,
                AnnotatedTypeMirror resultType) {

            Long divisor = IndexUtil.getExactValue(divisorTree, getValueAnnotatedTypeFactory());
            if (divisor == null) {
                resultType.addAnnotation(UNKNOWN);
                return;
            }

            UBQualifier result = UpperBoundUnknownQualifier.UNKNOWN;
            UBQualifier numerator =
                    UBQualifier.createUBQualifier(getAnnotatedType(numeratorTree), UNKNOWN);
            if (numerator.isLessThanLengthQualifier()) {
                result = ((LessThanLengthOf) numerator).divide(divisor.intValue());
            }
            result = result.glb(plusTreeDivideByVal(divisor.intValue(), numeratorTree));

            // If the numerator is an array length access of an array with non-zero length, and the divisor is
            // greater than one, glb the result with an LTL of the array.
            if (TreeUtils.isArrayLengthAccess(numeratorTree) && divisor > 1) {
                String arrayName = ((MemberSelectTree) numeratorTree).getExpression().toString();
                int minlen =
                        getMinLenAnnotatedTypeFactory()
                                .getMinLenFromString(
                                        arrayName, numeratorTree, getPath(numeratorTree));
                if (minlen > 0) {
                    result = result.glb(UBQualifier.createUBQualifier(arrayName, "0"));
                }
            }

            resultType.addAnnotation(convertUBQualifierToAnnotation(result));
        }

        /**
         * if numeratorTree is a + b and divisor greater than 1, and a and b are less than the
         * length of some array, then (a + b) / divisor is less than the length of that array.
         */
        private UBQualifier plusTreeDivideByVal(int divisor, ExpressionTree numeratorTree) {
            numeratorTree = TreeUtils.skipParens(numeratorTree);
            if (divisor < 2 || numeratorTree.getKind() != Kind.PLUS) {
                return UpperBoundUnknownQualifier.UNKNOWN;
            }
            BinaryTree plusTree = (BinaryTree) numeratorTree;
            UBQualifier left =
                    UBQualifier.createUBQualifier(
                            getAnnotatedType(plusTree.getLeftOperand()), UNKNOWN);
            UBQualifier right =
                    UBQualifier.createUBQualifier(
                            getAnnotatedType(plusTree.getRightOperand()), UNKNOWN);
            if (left.isLessThanLengthQualifier() && right.isLessThanLengthQualifier()) {
                LessThanLengthOf leftLTL = (LessThanLengthOf) left;
                LessThanLengthOf rightLTL = (LessThanLengthOf) right;
                List<String> arrays = new ArrayList<>();
                for (String array : leftLTL.getArrays()) {
                    if (rightLTL.isLessThanLengthOf(array) && leftLTL.isLessThanLengthOf(array)) {
                        arrays.add(array);
                    }
                }
                if (!arrays.isEmpty()) {
                    return UBQualifier.createUBQualifier(arrays, Collections.<String>emptyList());
                }
            }

            return UpperBoundUnknownQualifier.UNKNOWN;
        }

        private boolean checkForMathRandomSpecialCase(
                ExpressionTree randTree, ExpressionTree arrLenTree, AnnotatedTypeMirror type) {
            if (randTree.getKind() == Tree.Kind.METHOD_INVOCATION
                    && TreeUtils.isArrayLengthAccess(arrLenTree)) {
                MemberSelectTree mstree = (MemberSelectTree) arrLenTree;
                MethodInvocationTree mitree = (MethodInvocationTree) randTree;

                if (imf.isMathRandom(mitree, processingEnv)) {
                    // Okay, so this is Math.random() * array.length, which must be NonNegative
                    type.addAnnotation(
                            createLTLengthOfAnnotation(mstree.getExpression().toString()));
                    return true;
                }

                if (imf.isRandomNextDouble(mitree, processingEnv)) {
                    // Okay, so this is Random.nextDouble() * array.length, which must be NonNegative
                    type.addAnnotation(
                            createLTLengthOfAnnotation(mstree.getExpression().toString()));
                    return true;
                }
            }

            return false;
        }

        private void addAnnotationForMultiply(
                ExpressionTree leftExpr, ExpressionTree rightExpr, AnnotatedTypeMirror type) {
            // Special handling for multiplying an array length by a random variable.
            if (checkForMathRandomSpecialCase(rightExpr, leftExpr, type)
                    || checkForMathRandomSpecialCase(leftExpr, rightExpr, type)) {
                return;
            }
            type.addAnnotation(UNKNOWN);
        }
    }

    public AnnotationMirror convertUBQualifierToAnnotation(UBQualifier qualifier) {
        if (qualifier.isUnknown()) {
            return UNKNOWN;
        } else if (qualifier.isBottom()) {
            return BOTTOM;
        } else if (qualifier.isPoly()) {
            return POLY;
        }

        LessThanLengthOf ltlQualifier = (LessThanLengthOf) qualifier;
        return ltlQualifier.convertToAnnotationMirror(processingEnv);
    }
}
