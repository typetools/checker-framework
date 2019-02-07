package org.checkerframework.checker.index.lowerbound;

import static org.checkerframework.checker.index.IndexUtil.getPossibleValues;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import org.checkerframework.checker.index.IndexMethodIdentifier;
import org.checkerframework.checker.index.IndexUtil;
import org.checkerframework.checker.index.inequality.LessThanAnnotatedTypeFactory;
import org.checkerframework.checker.index.inequality.LessThanChecker;
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
import org.checkerframework.checker.index.qual.SubstringIndexFor;
import org.checkerframework.checker.index.searchindex.SearchIndexAnnotatedTypeFactory;
import org.checkerframework.checker.index.searchindex.SearchIndexChecker;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.common.value.qual.BottomVal;
import org.checkerframework.common.value.util.Range;
import org.checkerframework.dataflow.cfg.node.NumericalMultiplicationNode;
import org.checkerframework.framework.qual.PolyAll;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;
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
 * can, use that; if not, use more specific rules based on expression type. This class implements
 * the following type rules:
 *
 * <ul>
 *   <li>1. If the value checker type for any expression is &ge; 1, refine that expression's type to
 *       positive.
 *   <li>2. If the value checker type for any expression is &ge; 0 and case 1 did not apply, then
 *       refine that expression's type to non-negative.
 *   <li>3. If the value checker type for any expression is &ge; -1 and cases 1 and 2 did not apply,
 *       then refine that expression's type to GTEN1.
 *   <li>4. A unary prefix decrement shifts the type "down" in the hierarchy (i.e. {@code --i} when
 *       {@code i} is non-negative implies that {@code i} will be GTEN1 afterwards). Should this be
 *       3 rules?
 *   <li>5. A unary prefix increment shifts the type "up" in the hierarchy (i.e. {@code ++i} when
 *       {@code i} is non-negative implies that {@code i} will be positive afterwards). Should this
 *       be 3 rules?
 *   <li>6. Unary negation on a NegativeIndexFor from the SearchIndex type system results in a
 *       non-negative.
 *   <li>7. The result of a call to Math.max is the GLB of its arguments.
 *   <li>8. If an array has a MinLen type &ge; 1 and its length is accessed, the length expression
 *       is positive.
 * </ul>
 */
public class LowerBoundAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /** The canonical @{@link GTENegativeOne} annotation. */
    public final AnnotationMirror GTEN1 =
            AnnotationBuilder.fromClass(elements, GTENegativeOne.class);
    /** The canonical @{@link NonNegative} annotation. */
    public final AnnotationMirror NN = AnnotationBuilder.fromClass(elements, NonNegative.class);
    /** The canonical @{@link Positive} annotation. */
    public final AnnotationMirror POS = AnnotationBuilder.fromClass(elements, Positive.class);
    /** The bottom annotation. */
    public final AnnotationMirror BOTTOM =
            AnnotationBuilder.fromClass(elements, LowerBoundBottom.class);
    /** The canonical @{@link LowerBoundUnknown} annotation. */
    public final AnnotationMirror UNKNOWN =
            AnnotationBuilder.fromClass(elements, LowerBoundUnknown.class);
    /** The canonical @{@link PolyLowerBound} annotation. */
    public final AnnotationMirror POLY =
            AnnotationBuilder.fromClass(elements, PolyLowerBound.class);

    private final IndexMethodIdentifier imf;

    public LowerBoundAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        // Any annotations that are aliased to @NonNegative, @Positive,
        // or @GTENegativeOne must also be aliased in the constructor of
        // ValueAnnotatedTypeFactory to the appropriate @IntRangeFrom*
        // annotation.
        addAliasedAnnotation(IndexFor.class, NN);
        addAliasedAnnotation(IndexOrLow.class, GTEN1);
        addAliasedAnnotation(IndexOrHigh.class, NN);
        addAliasedAnnotation(LengthOf.class, NN);
        addAliasedAnnotation(PolyAll.class, POLY);
        addAliasedAnnotation(PolyIndex.class, POLY);
        addAliasedAnnotation(SubstringIndexFor.class, GTEN1);

        imf = new IndexMethodIdentifier(this);

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

    /** Handles cases 1, 2, and 3. */
    @Override
    public void addComputedTypeAnnotations(Element element, AnnotatedTypeMirror type) {
        super.addComputedTypeAnnotations(element, type);
        if (element != null) {
            AnnotatedTypeMirror valueType =
                    getValueAnnotatedTypeFactory().getAnnotatedType(element);
            addLowerBoundTypeFromValueType(valueType, type);
        }
    }

    /** Handles cases 1, 2, and 3. */
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

    /** Returns the LessThan Checker's annotated type factory. */
    public LessThanAnnotatedTypeFactory getLessThanAnnotatedTypeFactory() {
        return getTypeFactoryOfSubchecker(LessThanChecker.class);
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
    AnnotationMirror anmFromVal(long val) {
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
            } else { // Only unknown is left.
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

        /** Call increment and decrement helper functions. Handles cases 4, 5 and 6. */
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

        /** Special handling for Math.max. The return is the GLB of the arguments. Case 7. */
        @Override
        public Void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
            if (imf.isMathMax(tree)) {
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
         * For dealing with array length expressions. Looks for array length accesses specifically,
         * then dispatches to the MinLen checker to determine the length of the relevant array. If
         * it's found, use it to give the expression a type. Case 8.
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
         * Does not dispatch to binary operator helper methods. The Lower Bound Checker handles
         * binary operations via its transfer function.
         */
        @Override
        public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
            type.addAnnotation(UNKNOWN);
            return super.visitBinary(tree, type);
        }
    }

    /**
     * Looks up the minlen of a member select tree. Returns null if the tree doesn't represent an
     * array's length field.
     */
    Integer getMinLenFromMemberSelectTree(MemberSelectTree tree) {
        if (TreeUtils.isArrayLengthAccess(tree)) {
            return IndexUtil.getMinLenFromTree(tree, getValueAnnotatedTypeFactory());
        }
        return null;
    }

    /**
     * Looks up the minlen of a method invocation tree. Returns null if the tree doesn't represent
     * an string length method.
     */
    Integer getMinLenFromMethodInvocationTree(MethodInvocationTree tree) {
        if (imf.isLengthOfMethodInvocation(tree)) {
            return IndexUtil.getMinLenFromTree(tree, getValueAnnotatedTypeFactory());
        }
        return null;
    }

    /**
     * Given a multiplication, return its type if the LBC special-cases it, or null otherwise.
     *
     * <p>The LBC special-cases {@code Math.random() * array.length} and {@code Random.nextDouble()
     * * array.length}.
     *
     * @param node a multiplication node that may need special casing
     * @return an AnnotationMirror representing the result if the special case is valid, or null if
     *     not
     */
    AnnotationMirror checkForMathRandomSpecialCase(NumericalMultiplicationNode node) {
        AnnotationMirror forwardRes =
                checkForMathRandomSpecialCase(
                        node.getLeftOperand().getTree(), node.getRightOperand().getTree());
        if (forwardRes != null) {
            return forwardRes;
        }
        AnnotationMirror backwardsRes =
                checkForMathRandomSpecialCase(
                        node.getRightOperand().getTree(), node.getLeftOperand().getTree());
        if (backwardsRes != null) {
            return backwardsRes;
        }
        return null;
    }

    /**
     * Return true if randTree is a call to Math.random() or Random.nextDouble(), and arrLenTree is
     * someArray.length.
     */
    private AnnotationMirror checkForMathRandomSpecialCase(Tree randTree, Tree arrLenTree) {
        if (randTree.getKind() == Tree.Kind.METHOD_INVOCATION
                && TreeUtils.isArrayLengthAccess(arrLenTree)) {
            MethodInvocationTree miTree = (MethodInvocationTree) randTree;

            if (imf.isMathRandom(miTree, processingEnv)) {
                // This is Math.random() * array.length, which must be NonNegative
                return NN;
            }

            if (imf.isRandomNextDouble(miTree, processingEnv)) {
                // This is Random.nextDouble() * array.length, which must be NonNegative
                return NN;
            }
        }
        return null;
    }

    /** Checks if the expression is non-negative, i.e. it has Positive on NonNegative annotation. */
    public boolean isNonNegative(Tree tree) {
        // TODO: consolidate with the isNonNegative method in LowerBoundTransfer
        AnnotatedTypeMirror treeType = getAnnotatedType(tree);
        return treeType.hasAnnotation(NonNegative.class) || treeType.hasAnnotation(Positive.class);
    }
}
