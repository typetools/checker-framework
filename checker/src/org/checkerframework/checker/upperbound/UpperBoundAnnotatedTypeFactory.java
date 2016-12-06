package org.checkerframework.checker.upperbound;

import static org.checkerframework.javacutil.AnnotationUtils.getElementValueArray;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.IndexOrHigh;
import org.checkerframework.checker.minlen.MinLenAnnotatedTypeFactory;
import org.checkerframework.checker.minlen.MinLenChecker;
import org.checkerframework.checker.minlen.qual.*;
import org.checkerframework.checker.upperbound.qual.*;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ImplicitsTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Implements the introduction rules for the upper bound checker. Works primarily by way of querying
 * the minLen checker and comparing the min lengths of arrays to the known values of variables as
 * supplied by the value checker.
 */
public class UpperBoundAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /** Easy shorthand for UpperBoundUnknown.class, basically. */
    public static AnnotationMirror UNKNOWN;

    /**
     * Provides a way to query the Constant Value Checker, which computes the values of expressions
     * known at compile time (constant prop + folding).
     */
    private final ValueAnnotatedTypeFactory valueAnnotatedTypeFactory;

    /**
     * Provides a way to query the Min Len (minimum length) Checker, which computes the lengths of
     * arrays.
     */
    private final MinLenAnnotatedTypeFactory minLenAnnotatedTypeFactory;

    /** We need this to make an AnnotationBuilder for some reason. */
    protected static ProcessingEnvironment env;

    public UpperBoundAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        UNKNOWN = AnnotationUtils.fromClass(elements, UpperBoundUnknown.class);

        valueAnnotatedTypeFactory = getTypeFactoryOfSubchecker(ValueChecker.class);
        minLenAnnotatedTypeFactory = getTypeFactoryOfSubchecker(MinLenChecker.class);
        env = checker.getProcessingEnvironment();
        addAliasedAnnotation(IndexFor.class, createLTLengthOfAnnotation(new String[0]));
        addAliasedAnnotation(IndexOrHigh.class, createLTEqLengthOfAnnotation(new String[0]));
        this.postInit();
    }

    @Override
    public AnnotationMirror aliasedAnnotation(AnnotationMirror a) {
        if (AnnotationUtils.areSameByClass(a, IndexFor.class)) {
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
     * not, returns null.
     */
    public Integer minLenFromExpressionTree(ExpressionTree tree) {
        AnnotatedTypeMirror minLenType = minLenAnnotatedTypeFactory.getAnnotatedType(tree);
        AnnotationMirror anm = minLenType.getAnnotation(MinLen.class);
        if (anm == null) {
            return null;
        }
        Integer minLen = AnnotationUtils.getElementValue(anm, "value", Integer.class, true);
        return minLen;
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
            return new Integer(possibleValues.get(0).intValue());
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
        AnnotatedTypeMirror valueType = valueAnnotatedTypeFactory.getAnnotatedType(tree);
        List<Long> possibleValues = possibleValuesFromValueType(valueType);
        if (possibleValues == null || possibleValues.size() == 0) {
            return null;
        }
        // The annotation of the whole list is the max of the list.
        long valMax = Collections.max(possibleValues);
        return new Integer((int) valMax);
    }

    // I attempted to move all of these static methods into UpperBoundUtils on
    // 9.29.16. Do not try this. It does not work. They rely on the processing
    // environment, which is only available here. DO NOT TRY TO MOVE THEM.

    /**
     * Creates an annotation of the name given with the set of values given.
     *
     * @return annotation given by name with names=values, or UNKNOWN
     */
    private static AnnotationMirror createAnnotation(String name, String[] names) {
        if (names == null) {
            names = new String[0];
        }
        if (name.equals("LTLengthOf")) {
            return createLTLengthOfAnnotation(names);
        } else if (name.equals("LTEqLengthOf")) {
            return createLTEqLengthOfAnnotation(names);
        } else {
            return UNKNOWN;
        }
    }

    static AnnotationMirror createLTLengthOfAnnotation(String[] names) {
        AnnotationBuilder builder = new AnnotationBuilder(env, LTLengthOf.class);
        if (names == null) {
            names = new String[0];
        }
        builder.setValue("value", names);
        return builder.build();
    }

    static AnnotationMirror createLTLengthOfAnnotation(String name) {
        String[] names = {name};
        return createLTLengthOfAnnotation(names);
    }

    static AnnotationMirror createLTEqLengthOfAnnotation(String[] names) {
        AnnotationBuilder builder = new AnnotationBuilder(env, LTEqLengthOf.class);
        if (names == null) {
            names = new String[0];
        }
        builder.setValue("value", names);
        return builder.build();
    }

    static AnnotationMirror createLTEqLengthOfAnnotation(String name) {
        String[] names = {name};
        return createLTEqLengthOfAnnotation(names);
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
        Object[] values = newValues.toArray();
        String[] names = new String[values.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = values[i].toString();
        }
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
        HashSet<String> newValues = new HashSet<String>(Math.min(a1Names.size(), a2Names.size()));

        for (String s : a1Names) {
            if (a2Names.contains(s)) {
                newValues.add(s);
            }
        }

        Object[] values = newValues.toArray();
        String[] names = new String[values.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = values[i].toString();
        }
        return names;
    }

    /**
     * The qualifier hierarchy for the upperbound type system. The qh is responsible for determining
     * the relationships within the qualifiers - especially subtyping relations.
     */
    private final class UpperBoundQualifierHierarchy extends MultiGraphQualifierHierarchy {
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
                // This only works for LTL and LTEL. It must be one of the two.
                String[] names = getCombinedNames(a1, a2);

                if (AnnotationUtils.areSameByClass(a1, LTLengthOf.class)) {
                    return createLTLengthOfAnnotation(names);
                } else {
                    // This needs to be LTEL. If we change the type hierarchy, this has to change.
                    return createLTEqLengthOfAnnotation(names);
                }
            } else {
                /* If the two are unrelated, then the type hierarchy implies
                   that one is LTL and the other is LTEL, with different arrays,
                   meaning that the GLB
                   is LTEL of every array that is in either - since LTEL
                   is effectively the bottom type.
                */
                String[] names = getCombinedNames(a1, a2);
                return createLTEqLengthOfAnnotation(names);
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
            if (!AnnotationUtils.areSameIgnoringValues(
                    getTopAnnotation(a1), getTopAnnotation(a2))) {
                return null;
            } else if (isSubtype(a1, a2)) {
                return a2;
            } else if (isSubtype(a2, a1)) {
                return a1;
            }
            // If both are the same type, determine the type and merge:
            else if (AnnotationUtils.areSameIgnoringValues(a1, a2)) {

                String[] names = getIntersectingNames(a1, a2);
                return createAnnotation(a1.getAnnotationType().toString(), names);
            } else if ((AnnotationUtils.areSameByClass(a1, LTLengthOf.class)
                            && AnnotationUtils.areSameByClass(a2, LTEqLengthOf.class))
                    || (AnnotationUtils.areSameByClass(a1, LTEqLengthOf.class)
                            && AnnotationUtils.areSameByClass(a2, LTLengthOf.class))) {
                // In this case, the result should be LTEL of the intersection of the names.
                // Fixes issue 20.
                String[] names = getIntersectingNames(a1, a2);
                return createLTEqLengthOfAnnotation(names);
            }
            // Annotations are in this hierarchy, but they are not the same.
            else {
                return UNKNOWN;
            }
        }

        /**
         * Computes subtyping as per the subtyping in the qualifier hierarchy structure unless both
         * annotations are the same. In this case, rhs is a subtype of lhs iff lhs contains at least
         * every element of rhs.
         *
         * @return true if rhs is a subtype of lhs, false otherwise.
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
                 * rhs is a subtype of lhs iff lhs.value contains rhs.value.
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

            // To avoid doing evil things, we here enumerate all the conditions.
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

            // Neither is LTEL. Both must be EL, LTL, or Bottom. And the two must be
            // different. The only way this can return true is if rhs is Bottom.
            if (AnnotationUtils.areSameByClass(rhs, UpperBoundBottom.class)) {
                return true;
            }

            // Every other case results in false.
            return false;
        }
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                new UpperBoundTreeAnnotator(this),
                new PropagationTreeAnnotator(this),
                new ImplicitsTreeAnnotator(this));
    }

    protected class UpperBoundTreeAnnotator extends TreeAnnotator {

        public UpperBoundTreeAnnotator(UpperBoundAnnotatedTypeFactory factory) {
            super(factory);
        }

        /**
         * This exists specifically for Math.min. We need to special case Math.min because it has
         * unusual semantics: it can be used to combine annotations for the UBC, so it needs to be
         * special cased. Do not special case other methods here unless you have a compelling reason
         * to do so.
         */
        @Override
        public Void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
            ExecutableElement fcnMin =
                    TreeUtils.getMethod("java.lang.Math", "min", 2, processingEnv);
            if (TreeUtils.isMethodInvocation(tree, fcnMin, processingEnv)) {
                AnnotatedTypeMirror leftType = getAnnotatedType(tree.getArguments().get(0));
                AnnotatedTypeMirror rightType = getAnnotatedType(tree.getArguments().get(1));

                // If either is unknown or bottom, bail. We don't learn anything.
                if (leftType.hasAnnotation(UpperBoundUnknown.class)
                        || leftType.hasAnnotation(UpperBoundBottom.class)
                        || rightType.hasAnnotation(UpperBoundUnknown.class)
                        || rightType.hasAnnotation(UpperBoundBottom.class)) {
                    return super.visitMethodInvocation(tree, type);
                }
                // Now, both rightType and leftType are either LTL or LTEL.
                if (leftType.hasAnnotation(LTLengthOf.class)
                        && rightType.hasAnnotation(LTLengthOf.class)) {
                    // Both are LTL -> the result is LTL of the union.
                    AnnotationMirror leftAnno = leftType.getAnnotationInHierarchy(UNKNOWN);
                    AnnotationMirror rightAnno = rightType.getAnnotationInHierarchy(UNKNOWN);
                    String[] names = getCombinedNames(leftAnno, rightAnno);
                    type.replaceAnnotation(createLTLengthOfAnnotation(names));
                } else {
                    // Otherwise, one must be LTEL. This means that
                    // the result is LTEL of the union of the arrays.
                    AnnotationMirror leftAnno = leftType.getAnnotationInHierarchy(UNKNOWN);
                    AnnotationMirror rightAnno = rightType.getAnnotationInHierarchy(UNKNOWN);
                    String[] names = getCombinedNames(leftAnno, rightAnno);
                    type.replaceAnnotation(createLTEqLengthOfAnnotation(names));
                }
            }
            return super.visitMethodInvocation(tree, type);
        }

        @Override
        public Void visitMemberSelect(MemberSelectTree tree, AnnotatedTypeMirror type) {
            if (tree.getIdentifier().contentEquals("length")
                    && InternalUtils.typeOf(tree.getExpression()).getKind() == TypeKind.ARRAY) {
                String arrName = tree.getExpression().toString();
                type.replaceAnnotation(
                        qualHierarchy.greatestLowerBound(
                                createLTEqLengthOfAnnotation(arrName),
                                type.getAnnotationInHierarchy(UNKNOWN)));
            }
            return super.visitMemberSelect(tree, type);
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
         *     LTL / 1+ &rarr; LTL
         *     LTEL / 2+ &rarr; LTL
         *     LTEL / 1 &rarr; LTEL
         * </pre>
         */
        private void addAnnotationForDivide(
                ExpressionTree left, ExpressionTree right, AnnotatedTypeMirror type) {
            // Check if the right side's value is known at compile time.
            AnnotatedTypeMirror valueTypeRight = valueAnnotatedTypeFactory.getAnnotatedType(right);
            Integer maybeValRight = maybeValFromValueType(valueTypeRight);
            if (maybeValRight != null) {
                AnnotatedTypeMirror leftType = getAnnotatedType(left);
                addAnnotationForLiteralDivide(maybeValRight, leftType, type);
                return;
            }
        }

        private void addAnnotationForLiteralDivide(
                int val, AnnotatedTypeMirror nonLiteralType, AnnotatedTypeMirror type) {
            if (nonLiteralType.hasAnnotation(LTLengthOf.class)) {
                if (val >= 1) {
                    type.addAnnotation(nonLiteralType.getAnnotationInHierarchy(UNKNOWN));
                    return;
                }
            } else if (nonLiteralType.hasAnnotation(LTEqLengthOf.class)) {
                if (val >= 2) {
                    String[] names =
                            UpperBoundUtils.getValue(
                                    nonLiteralType.getAnnotationInHierarchy(UNKNOWN));
                    type.replaceAnnotation(createLTLengthOfAnnotation(names));
                    return;
                } else if (val == 1) {
                    type.addAnnotation(nonLiteralType.getAnnotationInHierarchy(UNKNOWN));
                    return;
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
                if (nonLiteralType.hasAnnotation(LTLengthOf.class)) {
                    String[] names =
                            UpperBoundUtils.getValue(
                                    nonLiteralType.getAnnotationInHierarchy(UNKNOWN));
                    type.replaceAnnotation(createLTEqLengthOfAnnotation(names));
                    return;
                }
                type.addAnnotation(UNKNOWN);
                return;
            }
            if (val < 0) {
                if (nonLiteralType.hasAnnotation(LTLengthOf.class)
                        || nonLiteralType.hasAnnotation(LTEqLengthOf.class)) {

                    String[] names =
                            UpperBoundUtils.getValue(
                                    nonLiteralType.getAnnotationInHierarchy(UNKNOWN));
                    type.replaceAnnotation(createLTLengthOfAnnotation(names));
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
         *      lit 1 + LTL &rarr; LTEL
         *      LTL,EL,LTEL + negative lit &rarr; LTL
         *      * + * &rarr; UNKNOWN
         *  </pre>
         */
        private void addAnnotationForPlus(
                ExpressionTree leftExpr, ExpressionTree rightExpr, AnnotatedTypeMirror type) {
            // Adding two literals isn't interesting, so we ignore it.
            AnnotatedTypeMirror leftType = getAnnotatedType(leftExpr);
            // Check if the right side's value is known at compile time.
            AnnotatedTypeMirror valueTypeRight =
                    valueAnnotatedTypeFactory.getAnnotatedType(rightExpr);
            Integer maybeValRight = maybeValFromValueType(valueTypeRight);
            if (maybeValRight != null) {
                addAnnotationForLiteralPlus(maybeValRight, leftType, type);
                return;
            }

            AnnotatedTypeMirror rightType = getAnnotatedType(rightExpr);
            // Check if the left side's value is known at compile time.
            AnnotatedTypeMirror valueTypeLeft =
                    valueAnnotatedTypeFactory.getAnnotatedType(rightExpr);
            Integer maybeValLeft = maybeValFromValueType(valueTypeLeft);
            if (maybeValLeft != null) {
                addAnnotationForLiteralPlus(maybeValLeft, rightType, type);
                return;
            }

            type.addAnnotation(UNKNOWN);
            return;
        }

        /**
         * Implements two rules: 1. If there is a literal on the right side of a subtraction, call
         * our literal add method, replacing the literal with the literal times negative one. 2.
         * Since EL implies that the number is either positive or zero, subtracting it from
         * something that's already LTL or EL always implies LTL, and from LTEL implies LTEL.
         */
        private void addAnnotationForMinus(
                ExpressionTree leftExpr, ExpressionTree rightExpr, AnnotatedTypeMirror type) {

            AnnotatedTypeMirror leftType = getAnnotatedType(leftExpr);
            // Check if the right side's value is known at compile time.
            AnnotatedTypeMirror valueTypeRight =
                    valueAnnotatedTypeFactory.getAnnotatedType(rightExpr);
            Integer maybeValRight = maybeValFromValueType(valueTypeRight);
            if (maybeValRight != null) {
                addAnnotationForLiteralPlus(-1 * maybeValRight, leftType, type);
                return;
            }
            //
            // I've commented this out because it relies on EqualToLength, which
            // I'm removing after talking with Joe and Mike.
            //
            // AnnotatedTypeMirror rightType = getAnnotatedType(rightExpr);
            // if (rightType.hasAnnotation(EqualToLength.class)) {
            //     if (leftType.hasAnnotation(EqualToLength.class)
            //             || leftType.hasAnnotation(LTLengthOf.class)) {
            //         String[] names =
            //                 UpperBoundUtils.getValue(leftType.getAnnotationInHierarchy(UNKNOWN));
            //         type.replaceAnnotation(createLTLengthOfAnnotation(names));
            //         return;
            //     }
            //     if (leftType.hasAnnotation(LTEqLengthOf.class)) {
            //         String[] names =
            //                 UpperBoundUtils.getValue(leftType.getAnnotationInHierarchy(UNKNOWN));
            //         type.replaceAnnotation(createLTEqLengthOfAnnotation(names));
            //         return;
            //     }
            // }
            type.addAnnotation(UNKNOWN);
            return;
        }
    }
}
