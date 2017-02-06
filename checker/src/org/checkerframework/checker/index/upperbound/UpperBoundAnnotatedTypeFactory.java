package org.checkerframework.checker.index.upperbound;

import static org.checkerframework.javacutil.AnnotationUtils.getElementValueArray;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.index.IndexMethodIdentifier;
import org.checkerframework.checker.index.minlen.MinLenAnnotatedTypeFactory;
import org.checkerframework.checker.index.minlen.MinLenChecker;
import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.IndexOrHigh;
import org.checkerframework.checker.index.qual.IndexOrLow;
import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.checker.index.qual.LTOMLengthOf;
import org.checkerframework.checker.index.qual.MinLen;
import org.checkerframework.checker.index.qual.UpperBoundBottom;
import org.checkerframework.checker.index.qual.UpperBoundUnknown;
import org.checkerframework.checker.index.samelen.SameLenAnnotatedTypeFactory;
import org.checkerframework.checker.index.samelen.SameLenChecker;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.framework.util.dependenttypes.DependentTypesHelper;
import org.checkerframework.framework.util.dependenttypes.DependentTypesTreeAnnotator;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Implements the introduction rules for the upper bound checker. Works primarily by way of querying
 * the minLen checker and comparing the min lengths of arrays to the known values of variables as
 * supplied by the value checker.
 */
public class UpperBoundAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /** Easy shorthand for UpperBoundUnknown.class, basically. */
    public final AnnotationMirror UNKNOWN;

    private final IndexMethodIdentifier imf;

    public UpperBoundAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        UNKNOWN = AnnotationUtils.fromClass(elements, UpperBoundUnknown.class);

        addAliasedAnnotation(IndexFor.class, createLTLengthOfAnnotation(new String[0]));
        addAliasedAnnotation(IndexOrLow.class, createLTLengthOfAnnotation(new String[0]));
        addAliasedAnnotation(IndexOrHigh.class, createLTEqLengthOfAnnotation(new String[0]));

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
                        UpperBoundBottom.class));
    }

    /**
     * Used to get the list of array names that an annotation applies to. Can return null if the
     * list would be empty.
     */
    public static String[] getValue(AnnotationMirror anno) {
        if (!AnnotationUtils.hasElementValue(anno, "value")) {
            return null;
        }
        return getElementValueArray(anno, "value", String.class, true).toArray(new String[0]);
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
    private SameLenAnnotatedTypeFactory getSameLenAnnotatedTypeFactory() {
        return getTypeFactoryOfSubchecker(SameLenChecker.class);
    }

    @Override
    protected DependentTypesHelper createDependentTypesHelper() {
        List<Class<? extends Annotation>> annos = new ArrayList<>();
        annos.add(LTLengthOf.class);
        annos.add(LTEqLengthOf.class);
        annos.add(IndexFor.class);
        annos.add(IndexOrLow.class);
        annos.add(IndexOrHigh.class);
        annos.add(LTOMLengthOf.class);
        return new DependentTypesHelper(this, annos) {
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
     * Queries the MinLen Checker to determine if there is a known minimum length for the array. If
     * not, returns -1.
     */
    public int minLenFromExpressionTree(ExpressionTree tree) {
        AnnotatedTypeMirror minLenType = getMinLenAnnotatedTypeFactory().getAnnotatedType(tree);
        AnnotationMirror anm = minLenType.getAnnotation(MinLen.class);
        if (anm == null) {
            return -1;
        }
        int minLen = AnnotationUtils.getElementValue(anm, "value", Integer.class, true);
        return minLen;
    }

    /**
     * Queries the SameLen Checker to return the type that the SameLen Checker associates with the
     * given expression tree.
     */
    public AnnotationMirror sameLenAnnotationFromExpressionTree(ExpressionTree tree) {
        AnnotatedTypeMirror sameLenType = getMinLenAnnotatedTypeFactory().getAnnotatedType(tree);
        return sameLenType.getAnnotationInHierarchy(UNKNOWN);
    }

    /** Get the list of possible values from a value checker type. May return null. */
    private List<Long> possibleValuesFromValueType(AnnotatedTypeMirror valueType) {
        AnnotationMirror anm = valueType.getAnnotation(IntVal.class);
        if (anm == null) {
            return null;
        }
        return ValueAnnotatedTypeFactory.getIntValues(anm);
    }

    /**
     * If the argument valueType indicates that the Constant Value Checker knows the exact value of
     * the annotated expression, returns that integer. Otherwise returns null. This method should
     * only be used by clients who need exactly one value - such as the binary operator rules - and
     * not by those that need to know whether a valueType belongs to a qualifier.
     */
    private Integer maybeValFromValueType(AnnotatedTypeMirror valueType) {
        List<Long> possibleValues = possibleValuesFromValueType(valueType);
        if (possibleValues != null && possibleValues.size() == 1) {
            return possibleValues.get(0).intValue();
        } else {
            return null;
        }
    }

    /** Finds the maximum value in the set of values represented by a value checker annotation. */
    public Integer valMaxFromExpressionTree(ExpressionTree tree) {
        /*  It's possible that possibleValues could be null (if
         *  there was no value checker annotation, I guess, but this
         *  definitely happens in practice) or empty (if the value
         *  checker annotated it with its equivalent of our unknown
         *  annotation.
         */
        AnnotatedTypeMirror valueType = getValueAnnotatedTypeFactory().getAnnotatedType(tree);
        List<Long> possibleValues = possibleValuesFromValueType(valueType);
        if (possibleValues == null || possibleValues.size() == 0) {
            return null;
        }
        // The annotation of the whole list is the max of the list.
        long valMax = Collections.max(possibleValues);
        return new Integer((int) valMax);
    }

    // Wrapper methods for accessing the IndexMethodIdentifier.

    public boolean isMathMin(Tree methodTree) {
        return imf.isMathMin(methodTree, processingEnv);
    }

    /**
     * Creates an annotation of the name given with the set of values given.
     *
     * @return annotation given by name with names=values, or UNKNOWN
     */
    protected AnnotationMirror createAnnotation(AnnotationMirror anno, String... names) {
        if (names == null) {
            names = new String[0];
        }
        if (AnnotationUtils.areSameByClass(anno, LTLengthOf.class)) {
            return createLTLengthOfAnnotation(names);
        } else if (AnnotationUtils.areSameByClass(anno, LTEqLengthOf.class)) {
            return createLTEqLengthOfAnnotation(names);
        } else if (AnnotationUtils.areSameByClass(anno, LTOMLengthOf.class)) {
            return createLTOMLengthOfAnnotation(names);
        } else {
            return UNKNOWN;
        }
    }

    /**
     * Creates an annotation of the name given with the set of values given.
     *
     * @return annotation given by name with names=values, or UNKNOWN
     */
    protected AnnotationMirror createAnnotation(Class<?> anno, String... names) {
        if (names == null) {
            names = new String[0];
        }
        if (LTLengthOf.class.equals(anno)) {
            return createLTLengthOfAnnotation(names);
        } else if (LTEqLengthOf.class.equals(anno)) {
            return createLTEqLengthOfAnnotation(names);
        } else if (LTOMLengthOf.class.equals(anno)) {
            return createLTOMLengthOfAnnotation(names);
        } else {
            return UNKNOWN;
        }
    }

    AnnotationMirror createLTOMLengthOfAnnotation(String... names) {
        AnnotationBuilder builder = new AnnotationBuilder(getProcessingEnv(), LTOMLengthOf.class);
        if (names == null) {
            names = new String[0];
        }
        builder.setValue("value", names);
        return builder.build();
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

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new UpperBoundQualifierHierarchy(factory);
    }

    /**
     * This function finds the union of the values of two annotations. Both annotations must have a
     * value field; otherwise the function will fail.
     *
     * @param a1 an annotation with a value field
     * @param a2 an annotation with a value field
     * @return the set union of the two value fields
     */
    private String[] getCombinedNames(AnnotationMirror a1, AnnotationMirror a2) {
        List<String> a1Names = getElementValueArray(a1, "value", String.class, true);
        List<String> a2Names = getElementValueArray(a2, "value", String.class, true);
        HashSet<String> newValues = new HashSet<String>(a1Names.size() + a2Names.size());

        newValues.addAll(a1Names);
        newValues.addAll(a2Names);
        String[] names = newValues.toArray(new String[0]);
        return names;
    }

    /**
     * This function finds the intersection of the values of two annotations. Both annotations must
     * have a value field; otherwise the function will fail.
     *
     * @param a1 an annotation with a value field
     * @param a2 an annotation with a value field
     * @return the set intersection of the two value fields
     */
    private String[] getIntersectingNames(AnnotationMirror a1, AnnotationMirror a2) {

        List<String> a1Names = getElementValueArray(a1, "value", String.class, true);
        List<String> a2Names = getElementValueArray(a2, "value", String.class, true);
        HashSet<String> newValues = new HashSet<>(Math.min(a1Names.size(), a2Names.size()));

        newValues.addAll(a1Names);
        newValues.retainAll(a2Names);

        String[] names = newValues.toArray(new String[0]);
        return names;
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
            if (isSubtype(a1, a2)) {
                return a1;
            } else if (isSubtype(a2, a1)) {
                return a2;
            } else if (AnnotationUtils.areSameIgnoringValues(a1, a2)) {
                String[] names = getCombinedNames(a1, a2);

                if (AnnotationUtils.areSameByClass(a1, LTLengthOf.class)) {
                    return createLTLengthOfAnnotation(names);
                } else if (AnnotationUtils.areSameByClass(a1, LTEqLengthOf.class)) {
                    return createLTEqLengthOfAnnotation(names);
                } else if (AnnotationUtils.areSameByClass(a1, LTOMLengthOf.class)) {
                    return createLTOMLengthOfAnnotation(names);
                } else {
                    return UNKNOWN; // Should never get here, but function has to be complete.
                }
            } else {
                /* If the two are unrelated, then the type hierarchy implies
                   that either: 1) one is LTL and the other is LTOM, so LTOM is glb
                   or 2) one of them is LTEL, so LTL is glb
                */
                String[] names = getCombinedNames(a1, a2);
                if (AnnotationUtils.areSameByClass(a2, LTOMLengthOf.class)
                        || AnnotationUtils.areSameByClass(a1, LTOMLengthOf.class)) {
                    return createLTOMLengthOfAnnotation(names);
                } else {
                    return createLTLengthOfAnnotation(names);
                }
            }
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
            if (isSubtype(a1, a2)) {
                return a2;
            } else if (isSubtype(a2, a1)) {
                return a1;
            }
            // If both are the same type, determine the type and merge:
            else if (AnnotationUtils.areSameIgnoringValues(a1, a2)) {

                String[] names = getIntersectingNames(a1, a2);
                return createAnnotation(a1, names);
            } else if ((AnnotationUtils.areSameByClass(a1, LTLengthOf.class)
                            && AnnotationUtils.areSameByClass(a2, LTEqLengthOf.class))
                    || (AnnotationUtils.areSameByClass(a1, LTEqLengthOf.class)
                            && AnnotationUtils.areSameByClass(a2, LTLengthOf.class))) {
                // In this case, the result should be LTEL of the intersection of the names.
                String[] names = getIntersectingNames(a1, a2);

                return createLTEqLengthOfAnnotation(names);
            } else if ((AnnotationUtils.areSameByClass(a1, LTOMLengthOf.class)
                            && AnnotationUtils.areSameByClass(a2, LTEqLengthOf.class))
                    || (AnnotationUtils.areSameByClass(a1, LTEqLengthOf.class)
                            && AnnotationUtils.areSameByClass(a2, LTOMLengthOf.class))) {

                String[] names = getIntersectingNames(a1, a2);
                return createLTEqLengthOfAnnotation(names);
            } else if ((AnnotationUtils.areSameByClass(a1, LTLengthOf.class)
                            && AnnotationUtils.areSameByClass(a2, LTOMLengthOf.class))
                    || (AnnotationUtils.areSameByClass(a1, LTOMLengthOf.class)
                            && AnnotationUtils.areSameByClass(a2, LTLengthOf.class))) {

                String[] names = getIntersectingNames(a1, a2);
                return createLTLengthOfAnnotation(names);
            }
            // Annotations are in this hierarchy, but they are not the same.
            else {
                return UNKNOWN;
            }
        }

        /**
         * Computes subtyping as per the subtyping in the qualifier hierarchy structure unless both
         * annotations are the same. In this case, rhs is a subtype of lhs iff rhs contains at least
         * every element of lhs.
         *
         * @return true if rhs is a subtype of lhs, false otherwise
         */
        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {

            if (AnnotationUtils.areSameByClass(lhs, UpperBoundUnknown.class)) {
                return true;
            } else if (AnnotationUtils.areSameByClass(rhs, UpperBoundUnknown.class)) {
                return false;
            } else if (AnnotationUtils.areSameByClass(rhs, UpperBoundBottom.class)) {
                return true;
            } else if (AnnotationUtils.areSameByClass(lhs, UpperBoundBottom.class)) {
                return false;
            } else if (AnnotationUtils.areSameIgnoringValues(lhs, rhs)) {
                // Same type, so might be subtype.
                List<Object> lhsValues = getElementValueArray(lhs, "value", Object.class, true);
                List<Object> rhsValues = getElementValueArray(rhs, "value", Object.class, true);
                return rhsValues.containsAll(lhsValues);
            } else if (isSubtypeRelaxed(rhs, lhs)) {
                /* Different types that are subtypes of each other ->
                 * rhs is a subtype of lhs iff rhs.value contains lhs.value.
                 */
                List<Object> lhsValues = getElementValueArray(lhs, "value", Object.class, true);
                List<Object> rhsValues = getElementValueArray(rhs, "value", Object.class, true);
                return rhsValues.containsAll(lhsValues);
            }
            return false;
        }

        // Gives subtyping information but ignores all values.
        private boolean isSubtypeRelaxed(AnnotationMirror rhs, AnnotationMirror lhs) {
            if (AnnotationUtils.areSameIgnoringValues(lhs, rhs)) {
                return true;
            }

            // Enumerate all the conditions.
            if (AnnotationUtils.areSameByClass(lhs, UpperBoundUnknown.class)) {
                return true;
            } else if (AnnotationUtils.areSameByClass(rhs, UpperBoundUnknown.class)) {
                return false;
            }
            // Neither is UB Unknown.
            if (AnnotationUtils.areSameByClass(lhs, LTEqLengthOf.class)) {
                return true;
            } else if (AnnotationUtils.areSameByClass(rhs, LTEqLengthOf.class)) {
                return false;
            }

            // Neither is LTEL. Both must be LTOM, LTL, or Bottom. And the two must be
            // different. The only way this can return true is if rhs is Bottom, or if rhs is
            // LTOM and lhs is LTL.
            if (AnnotationUtils.areSameByClass(rhs, UpperBoundBottom.class)) {
                return true;
            }

            if (AnnotationUtils.areSameByClass(rhs, LTOMLengthOf.class)
                    && AnnotationUtils.areSameByClass(lhs, LTLengthOf.class)) {
                return true;
            }

            // Every other case results in false.
            return false;
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
         * This exists specifically for Math.min. We need to special case Math.min because it has
         * unusual semantics: it can be used to combine annotations for the UBC, so it needs to be
         * special cased. Other methods should not be special-cased here unless there is a
         * compelling reason to do so.
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
            return super.visitMethodInvocation(tree, type);
        }

        @Override
        public Void visitMemberSelect(MemberSelectTree tree, AnnotatedTypeMirror type) {
            if (TreeUtils.isArrayLengthAccess(tree)) {
                String arrName =
                        FlowExpressions.internalReprOf(this.atypeFactory, tree.getExpression())
                                .toString();
                type.replaceAnnotation(createLTEqLengthOfAnnotation(arrName));
            }
            return super.visitMemberSelect(tree, type);
        }

        @Override
        public Void visitUnary(UnaryTree tree, AnnotatedTypeMirror typeDst) {
            AnnotatedTypeMirror typeSrc = getAnnotatedType(tree.getExpression());
            switch (tree.getKind()) {
                case PREFIX_INCREMENT:
                    handleIncrement(typeSrc, typeDst);
                    break;
                case PREFIX_DECREMENT:
                    handleDecrement(typeSrc, typeDst);
                    break;
                case POSTFIX_INCREMENT: // Do nothing. The CF should take care of these itself.
                    break;
                case POSTFIX_DECREMENT:
                    break;
                default:
                    break;
            }
            return super.visitUnary(tree, typeDst);
        }

        private void handleIncrement(AnnotatedTypeMirror typeSrc, AnnotatedTypeMirror typeDst) {
            if (typeSrc.hasAnnotation(LTOMLengthOf.class)) {
                String[] names = getValue(typeSrc.getAnnotationInHierarchy(UNKNOWN));
                typeDst.replaceAnnotation(createLTLengthOfAnnotation(names));
            } else if (typeSrc.hasAnnotation(LTLengthOf.class)) {
                String[] names = getValue(typeSrc.getAnnotationInHierarchy(UNKNOWN));
                typeDst.replaceAnnotation(createLTEqLengthOfAnnotation(names));
            } else if (typeSrc.hasAnnotation(LTEqLengthOf.class)) {
                typeDst.replaceAnnotation(UNKNOWN);
            }
        }

        private void handleDecrement(AnnotatedTypeMirror typeSrc, AnnotatedTypeMirror typeDst) {
            if (typeSrc.hasAnnotation(LTLengthOf.class)
                    || typeSrc.hasAnnotation(LTOMLengthOf.class)) {
                String[] names = getValue(typeSrc.getAnnotationInHierarchy(UNKNOWN));
                typeDst.replaceAnnotation(createLTOMLengthOfAnnotation(names));
            } else if (typeSrc.hasAnnotation(LTEqLengthOf.class)) {
                String[] names = getValue(typeSrc.getAnnotationInHierarchy(UNKNOWN));
                typeDst.replaceAnnotation(createLTLengthOfAnnotation(names));
            }
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
                default:
                    break;
            }
            return super.visitBinary(tree, type);
        }

        /**
         * Handles these cases:
         *
         * <pre>
         *     LTOM / 1+ &rarr; LTOM
         *     LTL / 1+ &rarr; LTL
         *     LTEL / 2+ &rarr; LTL
         *     LTEL / 1 &rarr; LTEL
         * </pre>
         */
        private void addAnnotationForDivide(
                ExpressionTree left, ExpressionTree right, AnnotatedTypeMirror type) {
            // Check if the right side's value is known at compile time.
            AnnotatedTypeMirror valueTypeRight =
                    getValueAnnotatedTypeFactory().getAnnotatedType(right);
            Integer maybeValRight = maybeValFromValueType(valueTypeRight);
            if (maybeValRight != null) {
                AnnotatedTypeMirror leftType = getAnnotatedType(left);
                addAnnotationForLiteralDivide(maybeValRight, leftType, type);
            }
        }

        private void addAnnotationForLiteralDivide(
                int val, AnnotatedTypeMirror nonLiteralType, AnnotatedTypeMirror type) {
            if (nonLiteralType.hasAnnotation(LTLengthOf.class)
                    || nonLiteralType.hasAnnotation(LTOMLengthOf.class)) {
                if (val >= 1) {
                    type.replaceAnnotation(nonLiteralType.getAnnotationInHierarchy(UNKNOWN));
                }
            } else if (nonLiteralType.hasAnnotation(LTEqLengthOf.class)) {
                // FIXME: Is this unsafe? What if the length is zero?
                if (val >= 2) {
                    String[] names = getValue(nonLiteralType.getAnnotationInHierarchy(UNKNOWN));
                    type.replaceAnnotation(createLTLengthOfAnnotation(names));
                } else if (val == 1) {
                    type.addAnnotation(nonLiteralType.getAnnotationInHierarchy(UNKNOWN));
                }
            }
        }

        private void addAnnotationForLiteralPlus(
                int val, AnnotatedTypeMirror nonLiteralType, AnnotatedTypeMirror type) {
            if (val == 0) {
                type.addAnnotation(nonLiteralType.getAnnotationInHierarchy(UNKNOWN));
                return;
            }
            if (val == 1) {
                handleIncrement(nonLiteralType, type);
                return;
            }
            if (val == -1) {
                handleDecrement(nonLiteralType, type);
                return;
            }
            if (val < -1) {
                if (nonLiteralType.hasAnnotation(LTLengthOf.class)
                        || nonLiteralType.hasAnnotation(LTOMLengthOf.class)
                        || nonLiteralType.hasAnnotation(LTEqLengthOf.class)) {

                    String[] names = getValue(nonLiteralType.getAnnotationInHierarchy(UNKNOWN));
                    type.replaceAnnotation(createLTOMLengthOfAnnotation(names));
                    return;
                }
                type.addAnnotation(UNKNOWN);
                return;
            }
            // Covers positive numbers.
            type.addAnnotation(UNKNOWN);
            return;
        }

        /**
         * addAnnotationForPlus handles the following cases:
         *
         * <pre>
         *      lit 0 + * &rarr; *
         *      lit 1 + * &rarr; call increment
         *      lit -1 + * &rarr; call decrement
         *      LTL,LTOM,LTEL + negative lit &lt; -1 &rarr; LTOM
         *      * + * &rarr; UNKNOWN
         *  </pre>
         */
        private void addAnnotationForPlus(
                ExpressionTree leftExpr, ExpressionTree rightExpr, AnnotatedTypeMirror type) {
            // Adding two literals isn't interesting, so ignore it.
            AnnotatedTypeMirror leftType = getAnnotatedType(leftExpr);
            // Check if the right side's value is known at compile time.
            AnnotatedTypeMirror valueTypeRight =
                    getValueAnnotatedTypeFactory().getAnnotatedType(rightExpr);
            Integer maybeValRight = maybeValFromValueType(valueTypeRight);
            if (maybeValRight != null) {
                addAnnotationForLiteralPlus(maybeValRight, leftType, type);
                return;
            }

            AnnotatedTypeMirror rightType = getAnnotatedType(rightExpr);
            // Check if the left side's value is known at compile time.
            AnnotatedTypeMirror valueTypeLeft =
                    getValueAnnotatedTypeFactory().getAnnotatedType(rightExpr);
            Integer maybeValLeft = maybeValFromValueType(valueTypeLeft);
            if (maybeValLeft != null) {
                addAnnotationForLiteralPlus(maybeValLeft, rightType, type);
                return;
            }

            type.addAnnotation(UNKNOWN);
            return;
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

            return;
        }

        /**
         * Implements two rules: 1. If there is a literal on the right side of a subtraction, call
         * our literal add method, replacing the literal with the literal times negative one. 2.
         */
        private void addAnnotationForMinus(
                ExpressionTree leftExpr, ExpressionTree rightExpr, AnnotatedTypeMirror type) {

            AnnotatedTypeMirror leftType = getAnnotatedType(leftExpr);
            // Check if the right side's value is known at compile time.
            AnnotatedTypeMirror valueTypeRight =
                    getValueAnnotatedTypeFactory().getAnnotatedType(rightExpr);
            Integer maybeValRight = maybeValFromValueType(valueTypeRight);
            if (maybeValRight != null) {
                addAnnotationForLiteralPlus(-1 * maybeValRight, leftType, type);
                return;
            }
            type.addAnnotation(UNKNOWN);
            return;
        }
    }

    /**
     * Combines the facts in a1 with those in a2.
     *
     * <p>Same algorithm as greatestLowerBound except if neither annotation is {@link
     * UpperBoundUnknown} nor {@link UpperBoundBottom} and the arrays in the annotation are not the
     * same, then the returned annotation is the higher of the two and the union of the arrays. For
     * example, given {@code @LTLengthOf({"a1", "a2"})} and {@code @LTEqLengthOf ({"b1", "b2"})}
     * this method returns {@code @LTEqLengthOf({"a1", "a2","b1", "b2"})}}.
     *
     * @param a1 AnnotationMirror
     * @param a2 AnnotationMirror
     * @return combines the facts in a1 with those in a2
     */
    public AnnotationMirror combineFacts(AnnotationMirror a1, AnnotationMirror a2) {
        if (qualHierarchy.isSubtype(a1, a2)) {
            return a1;
        } else if (qualHierarchy.isSubtype(a2, a1)) {
            return a2;
        } else if (AnnotationUtils.areSameIgnoringValues(a1, a2)) {
            String[] names = getCombinedNames(a1, a2);

            if (AnnotationUtils.areSameByClass(a1, LTLengthOf.class)) {
                return createLTLengthOfAnnotation(names);
            } else if (AnnotationUtils.areSameByClass(a1, LTEqLengthOf.class)) {
                return createLTEqLengthOfAnnotation(names);
            } else if (AnnotationUtils.areSameByClass(a1, LTOMLengthOf.class)) {
                return createLTOMLengthOfAnnotation(names);
            } else {
                return UNKNOWN; // Should never get here, but function has to be complete.
            }
        } else {
            // a1 and a2 are not annotations of the same class.
            // Also, one isn't a subtype of the other, so the arrays in each annotation is
            // different. So, use the combined names, but the annotation has to be the higher of
            // the two.
            String[] names = getCombinedNames(a1, a2);
            if (AnnotationUtils.areSameByClass(a2, LTEqLengthOf.class)
                    || AnnotationUtils.areSameByClass(a1, LTEqLengthOf.class)) {
                // If either annotation is LTEqL, then
                return createLTEqLengthOfAnnotation(names);
            } else {
                return createLTLengthOfAnnotation(names);
            }
        }
    }
}
