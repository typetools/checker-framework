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
import javax.lang.model.element.Element;
import org.checkerframework.checker.index.IndexMethodIdentifier;
import org.checkerframework.checker.index.IndexUtil;
import org.checkerframework.checker.index.lowerbound.LowerBoundAnnotatedTypeFactory;
import org.checkerframework.checker.index.lowerbound.LowerBoundChecker;
import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.IndexOrHigh;
import org.checkerframework.checker.index.qual.IndexOrLow;
import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.checker.index.qual.LTOMLengthOf;
import org.checkerframework.checker.index.qual.LengthOf;
import org.checkerframework.checker.index.qual.NegativeIndexFor;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.PolyIndex;
import org.checkerframework.checker.index.qual.PolyUpperBound;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.index.qual.SameLen;
import org.checkerframework.checker.index.qual.SearchIndexFor;
import org.checkerframework.checker.index.qual.UpperBoundBottom;
import org.checkerframework.checker.index.qual.UpperBoundUnknown;
import org.checkerframework.checker.index.samelen.SameLenAnnotatedTypeFactory;
import org.checkerframework.checker.index.samelen.SameLenChecker;
import org.checkerframework.checker.index.searchindex.SearchIndexAnnotatedTypeFactory;
import org.checkerframework.checker.index.searchindex.SearchIndexChecker;
import org.checkerframework.checker.index.upperbound.UBQualifier.LessThanLengthOf;
import org.checkerframework.checker.index.upperbound.UBQualifier.UpperBoundUnknownQualifier;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.common.value.qual.BottomVal;
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
        addAliasedAnnotation(SearchIndexFor.class, createLTLengthOfAnnotation());
        addAliasedAnnotation(NegativeIndexFor.class, createLTLengthOfAnnotation());
        addAliasedAnnotation(LengthOf.class, createLTEqLengthOfAnnotation());
        addAliasedAnnotation(PolyAll.class, POLY);
        addAliasedAnnotation(PolyIndex.class, POLY);

        imf = new IndexMethodIdentifier(processingEnv);

        this.postInit();
    }

    /** Gets a helper object that holds references to methods with special handling. */
    IndexMethodIdentifier getMethodIdentifier() {
        return imf;
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
     * known at compile time (constant propagation and folding).
     */
    ValueAnnotatedTypeFactory getValueAnnotatedTypeFactory() {
        return getTypeFactoryOfSubchecker(ValueChecker.class);
    }

    /**
     * Provides a way to query the Search Index Checker, which helps the Index Checker type the
     * results of calling the JDK's binary search methods correctly.
     */
    private SearchIndexAnnotatedTypeFactory getSearchIndexAnnotatedTypeFactory() {
        return getTypeFactoryOfSubchecker(SearchIndexChecker.class);
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
    public void addComputedTypeAnnotations(Element element, AnnotatedTypeMirror type) {
        super.addComputedTypeAnnotations(element, type);
        if (element != null) {
            AnnotatedTypeMirror valueType =
                    getValueAnnotatedTypeFactory().getAnnotatedType(element);
            addUpperBoundTypeFromValueType(valueType, type);
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
            addUpperBoundTypeFromValueType(valueType, type);
        }
    }

    /**
     * Checks if valueType contains a {@link org.checkerframework.common.value.qual.BottomVal}
     * annotation. If so, adds an {@link UpperBoundBottom} annotation to type.
     */
    private void addUpperBoundTypeFromValueType(
            AnnotatedTypeMirror valueType, AnnotatedTypeMirror type) {
        if (AnnotationUtils.containsSameByClass(valueType.getAnnotations(), BottomVal.class)) {
            type.replaceAnnotation(BOTTOM);
        }
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
        if (AnnotationUtils.areSameByClass(a, IndexFor.class)
                || AnnotationUtils.areSameByClass(a, SearchIndexFor.class)
                || AnnotationUtils.areSameByClass(a, NegativeIndexFor.class)) {
            List<String> stringList =
                    AnnotationUtils.getElementValueArray(a, "value", String.class, true);
            return createLTLengthOfAnnotation(stringList.toArray(new String[0]));
        }
        if (AnnotationUtils.areSameByClass(a, IndexOrLow.class)) {
            List<String> stringList =
                    AnnotationUtils.getElementValueArray(a, "value", String.class, true);
            return createLTLengthOfAnnotation(stringList.toArray(new String[0]));
        }
        if (AnnotationUtils.areSameByClass(a, IndexOrHigh.class)
                || AnnotationUtils.areSameByClass(a, LengthOf.class)) {
            List<String> stringList =
                    AnnotationUtils.getElementValueArray(a, "value", String.class, true);
            return createLTEqLengthOfAnnotation(stringList.toArray(new String[0]));
        }
        return super.aliasedAnnotation(a);
    }

    /**
     * Queries the SameLen Checker to return the type that the SameLen Checker associates with the
     * given tree.
     */
    public AnnotationMirror sameLenAnnotationFromTree(Tree tree) {
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
        public AnnotationMirror widenedUpperBound(
                AnnotationMirror newQualifier, AnnotationMirror previousQualifier) {
            UBQualifier a1Obj = UBQualifier.createUBQualifier(newQualifier);
            UBQualifier a2Obj = UBQualifier.createUBQualifier(previousQualifier);
            UBQualifier lub = a1Obj.widenUpperBound(a2Obj);
            return convertUBQualifierToAnnotation(lub);
        }

        @Override
        public int numberOfIterationsBeforeWidening() {
            return 10;
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
            if (node.getKind() == Kind.BITWISE_COMPLEMENT) {
                addAnnotationForBitwiseComplement(
                        getSearchIndexAnnotatedTypeFactory().getAnnotatedType(node.getExpression()),
                        type);
            } else {
                type.addAnnotation(UNKNOWN);
            }
            return super.visitUnary(node, type);
        }

        /**
         * If a type returned by an {@link SearchIndexAnnotatedTypeFactory} has a {@link
         * NegativeIndexFor} annotation, then refine the result to be {@link LTEqLengthOf}. This
         * handles this case:
         *
         * <pre>{@code
         * int i = Arrays.binarySearch(a, x);
         * if (i >= 0) {
         *     // do something
         * } else {
         *     i = ~i;
         *     // i is now @LTEqLengthOf("a"), because the bitwise complement of a NegativeIndexFor is an LTL.
         *     for (int j = 0; j < i; j++) {
         *          // j is now a valid index for "a"
         *     }
         * }
         * }</pre>
         *
         * @param searchIndexType the type of an expression in a bitwise complement. For instance,
         *     in {@code ~x}, this is the type of {@code x}.
         * @param typeDst the type of the entire bitwise complement expression. It is modified by
         *     this method.
         */
        private void addAnnotationForBitwiseComplement(
                AnnotatedTypeMirror searchIndexType, AnnotatedTypeMirror typeDst) {
            if (AnnotationUtils.containsSameByClass(
                    searchIndexType.getAnnotations(), NegativeIndexFor.class)) {
                AnnotationMirror nif = searchIndexType.getAnnotation(NegativeIndexFor.class);
                List<String> arrays = IndexUtil.getValueOfAnnotationWithStringArgument(nif);
                List<String> negativeOnes = Collections.nCopies(arrays.size(), "-1");
                UBQualifier qual = UBQualifier.createUBQualifier(arrays, negativeOnes);
                typeDst.addAnnotation(convertUBQualifierToAnnotation(qual));
            } else {
                typeDst.addAnnotation(UNKNOWN);
            }
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

        /** Gets a sequence tree for a length access tree, or null if it is not a length access. */
        private ExpressionTree getLengthSequenceTree(ExpressionTree lengthTree) {
            return IndexUtil.getLengthSequenceTree(lengthTree, imf, processingEnv);
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

            ExpressionTree numeratorSequenceTree = getLengthSequenceTree(numeratorTree);
            // If the numerator is an array length access of an array with non-zero length, and the divisor is
            // greater than one, glb the result with an LTL of the array.
            if (numeratorSequenceTree != null && divisor > 1) {
                String arrayName = numeratorSequenceTree.toString();
                int minlen =
                        getValueAnnotatedTypeFactory()
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
         * length of some sequence, then (a + b) / divisor is less than the length of that sequence.
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
                List<String> sequences = new ArrayList<>();
                for (String sequence : leftLTL.getSequences()) {
                    if (rightLTL.isLessThanLengthOf(sequence)
                            && leftLTL.isLessThanLengthOf(sequence)) {
                        sequences.add(sequence);
                    }
                }
                if (!sequences.isEmpty()) {
                    return UBQualifier.createUBQualifier(
                            sequences, Collections.<String>emptyList());
                }
            }

            return UpperBoundUnknownQualifier.UNKNOWN;
        }

        private boolean checkForMathRandomSpecialCase(
                ExpressionTree randTree, ExpressionTree seqLenTree, AnnotatedTypeMirror type) {

            ExpressionTree seqTree = getLengthSequenceTree(seqLenTree);

            if (randTree.getKind() == Tree.Kind.METHOD_INVOCATION && seqTree != null) {

                MethodInvocationTree mitree = (MethodInvocationTree) randTree;

                if (imf.isMathRandom(mitree, processingEnv)) {
                    // Okay, so this is Math.random() * array.length, which must be NonNegative
                    type.addAnnotation(createLTLengthOfAnnotation(seqTree.toString()));
                    return true;
                }

                if (imf.isRandomNextDouble(mitree, processingEnv)) {
                    // Okay, so this is Random.nextDouble() * array.length, which must be NonNegative
                    type.addAnnotation(createLTLengthOfAnnotation(seqTree.toString()));
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
