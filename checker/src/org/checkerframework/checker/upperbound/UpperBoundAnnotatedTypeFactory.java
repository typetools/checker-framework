package org.checkerframework.checker.upperbound;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.minlen.MinLenAnnotatedTypeFactory;
import org.checkerframework.checker.minlen.MinLenChecker;
import org.checkerframework.checker.minlen.qual.*;
import org.checkerframework.checker.upperbound.qual.*;
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
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.Pair;

/**
 * Implements the introduction rules for the upper bound checker.
 * Works primarily by way of querying the minLen checker
 * and comparing the min lengths of arrays to the known values
 * of variables as supplied by the value checker.
 */
public class UpperBoundAnnotatedTypeFactory
        extends GenericAnnotatedTypeFactory<
                CFValue, CFStore, UpperBoundTransfer, UpperBoundAnalysis> {

    /**
     *  So Suzanne told me these were evil, but then I ended up using them
     *  to correctly implement the subtyping relation that I wanted. I'll get
     *  rid of them if I can figure out a better way to do that, but for now
     *  they stay.
     */
    public static AnnotationMirror LTL, LTEL, EL, UNKNOWN;

    /**
     *  Provides a way to query the Constant Value Checker, which computes the
     *  values of expressions known at compile time (constant prop + folding).
     */
    private final ValueAnnotatedTypeFactory valueAnnotatedTypeFactory;

    /**
     *  Provides a way to query the Min Len (minimum length) Checker,
     *  which computes the lengths of arrays.
     */
    private final MinLenAnnotatedTypeFactory minLenAnnotatedTypeFactory;

    /**
     *  We need this to make an AnnotationBuilder for some reason.
     */
    protected static ProcessingEnvironment env;

    public UpperBoundAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        LTL = AnnotationUtils.fromClass(elements, LessThanLength.class);
        LTEL = AnnotationUtils.fromClass(elements, LessThanOrEqualToLength.class);
        EL = AnnotationUtils.fromClass(elements, EqualToLength.class);
        UNKNOWN = AnnotationUtils.fromClass(elements, UpperBoundUnknown.class);

        valueAnnotatedTypeFactory = getTypeFactoryOfSubchecker(ValueChecker.class);
        minLenAnnotatedTypeFactory = getTypeFactoryOfSubchecker(MinLenChecker.class);
        env = checker.getProcessingEnvironment();
        this.postInit();
    }

    /**
     *  Queries the MinLen Checker to determine if there
     *  is a known minimum length for the array. If not,
     *  returns null.
     */
    public Integer minLenFromExpressionTree(ExpressionTree tree) {
        AnnotatedTypeMirror minLenType = minLenAnnotatedTypeFactory.getAnnotatedType(tree);
        AnnotationMirror anm = minLenType.getAnnotation(MinLen.class);
        Integer minLen = AnnotationUtils.getElementValue(anm, "value", Integer.class, true);
        return minLen;
    }

    /**
     *  Get the list of possible values from a value checker type.
     *  May return null.
     */
    private List<Long> possibleValuesFromValueType(AnnotatedTypeMirror valueType) {
        AnnotationMirror anm = valueType.getAnnotation(IntVal.class);
        if (anm == null) {
            return null;
        }
        return ValueAnnotatedTypeFactory.getIntValues(anm);
    }

    /**
     * If the argument valueType indicates that the Constant Value
     * Checker knows the exact value of the annotated expression,
     * returns that integer.  Otherwise returns null. This method
     * should only be used by clients who need exactly one value -
     * such as the binary operator rules - and not by those that
     * need to know whether a valueType belongs to a qualifier.
     */
    private Integer maybeValFromValueType(AnnotatedTypeMirror valueType) {
        List<Long> possibleValues = possibleValuesFromValueType(valueType);
        if (possibleValues != null && possibleValues.size() == 1) {
            return new Integer(possibleValues.get(0).intValue());
        } else {
            return null;
        }
    }

    /**
     *  Finds the maximum value in the set of values represented
     *  by a value checker annotation.
     */
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

    @Override
    protected UpperBoundAnalysis createFlowAnalysis(
            List<Pair<VariableElement, CFValue>> fieldValues) {
        return new UpperBoundAnalysis(checker, this, fieldValues);
    }

    // FIXME: In an unsurprising turn of events, this isn't working.
    // Going to ignore it for now and use specialized ones but we
    // should come back and fix later...

    /**
     * Creates an annotation of the name given with the set of values given.
     * Exists in place of a series of createXAnnotation methods because that
     * would be silly.
     *
     * @return annotation given by name with names=values, or UNKNOWN
     */
    private static AnnotationMirror createAnnotation(String name, Set<?> values) {
        if (values.size() > 0) {
            AnnotationBuilder builder = new AnnotationBuilder(env, name);
            List<Object> valuesList = new ArrayList<Object>(values);
            builder.setValue("value", valuesList);
            return builder.build();
        } else {
            return UNKNOWN;
        }
    }

    private static AnnotationMirror createAnnotation(String name, String[] values) {
        return createAnnotation(name, new HashSet<String>(Arrays.asList(values)));
    }

    static AnnotationMirror createLessThanLengthAnnotation(String[] names) {
        AnnotationBuilder builder = new AnnotationBuilder(env, LessThanLength.class);
        builder.setValue("value", names);
        return builder.build();
    }

    static AnnotationMirror createLessThanLengthAnnotation(String name) {
        String[] names = {name};
        return createLessThanLengthAnnotation(names);
    }

    static AnnotationMirror createEqualToLengthAnnotation(String[] names) {
        AnnotationBuilder builder = new AnnotationBuilder(env, EqualToLength.class);
        builder.setValue("value", names);
        return builder.build();
    }

    static AnnotationMirror createEqualToLengthAnnotation(String name) {
        String[] names = {name};
        return createEqualToLengthAnnotation(names);
    }

    static AnnotationMirror createLessThanOrEqualToLengthAnnotation(String[] names) {
        AnnotationBuilder builder = new AnnotationBuilder(env, LessThanOrEqualToLength.class);
        builder.setValue("value", names);
        return builder.build();
    }

    static AnnotationMirror createLessThanOrEqualToLengthAnnotation(String name) {
        String[] names = {name};
        return createLessThanOrEqualToLengthAnnotation(names);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new UpperBoundQualifierHierarchy(factory);
    }

    /**
     * The qualifier hierarchy for the upperbound type system.
     * The qh is responsible for determining the relationships
     * within the qualifiers - especially subtyping relations.
     */
    private final class UpperBoundQualifierHierarchy extends MultiGraphQualifierHierarchy {
        /**
         * @param factory
         *            MultiGraphFactory to use to construct this
         */
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
            } else {
                /* If the two are unrelated, then the type hierarchy implies
                   that one is LTL and the other is EL, meaning that the GLB
                   is LTEL of every array that is in either - since LTEL
                   is the bottom type.
                */
                List<Object> a1Names =
                        AnnotationUtils.getElementValueArray(a1, "value", Object.class, true);
                List<Object> a2Names =
                        AnnotationUtils.getElementValueArray(a2, "value", Object.class, true);
                HashSet<Object> newValues = new HashSet<Object>(a1Names.size() + a2Names.size());

                newValues.addAll(a1Names);
                newValues.addAll(a2Names);
                Object[] values = newValues.toArray();
                String[] names = new String[values.length];
                for (int i = 0; i < names.length; i++) {
                    names[i] = values[i].toString();
                }

                return createLessThanOrEqualToLengthAnnotation(names);
            }
        }

        /**
         *  Finds the minimum value in the set of values represented
         *  by a value checker annotation.
         */
        Integer valMinFromValueType(AnnotatedTypeMirror valueType) {
            /*  It's possible that possibleValues could be null (if
             *  there was no value checker annotation, I guess, but this
             *  definitely happens in practice) or empty (if the value
             *  checker annotated it with its equivalent of our unknown
             *  annotation.
             */
            List<Long> possibleValues = possibleValuesFromValueType(valueType);
            if (possibleValues == null || possibleValues.size() == 0) {
                return null;
            }
            // The annotation of the whole list is the min of the list.
            long valMin = Collections.min(possibleValues);
            return new Integer((int) valMin);
        }

        /**
         * Determines the least upper bound of a1 and a2. If a1 and a2 are both
         * the same type of Value annotation, then the LUB is the result of
         * taking all values from both a1 and a2 and removing duplicates.
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
                List<Object> a1Values =
                        AnnotationUtils.getElementValueArray(a1, "value", Object.class, true);
                List<Object> a2Values =
                        AnnotationUtils.getElementValueArray(a2, "value", Object.class, true);
                HashSet<Object> newValues = new HashSet<Object>(a1Values.size() + a2Values.size());

                newValues.addAll(a1Values);
                newValues.addAll(a2Values);

                return createAnnotation(a1.getAnnotationType().toString(), newValues);
            }
            // Annotations are in this hierarchy, but they are not the same.
            else {
                return UNKNOWN;
            }
        }

        /**
         * Computes subtyping as per the subtyping in the qualifier hierarchy
         * structure unless both annotations are the same. In this case, rhs is a
         * subtype of lhs iff lhs contains at least every element of rhs.
         *
         * @return true if rhs is a subtype of lhs, false otherwise.
         */
        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {

            if (AnnotationUtils.areSameByClass(lhs, UpperBoundUnknown.class)) {
                return true;
            } else if (AnnotationUtils.areSameByClass(rhs, UpperBoundUnknown.class)) {
                return false;
            } else if (AnnotationUtils.areSameIgnoringValues(lhs, rhs)) {
                // Same type, so might be subtype.
                List<Object> lhsValues =
                        AnnotationUtils.getElementValueArray(lhs, "value", Object.class, true);
                List<Object> rhsValues =
                        AnnotationUtils.getElementValueArray(rhs, "value", Object.class, true);
                return rhsValues.containsAll(lhsValues);
            } else if (isSubtypeRelaxed(rhs, lhs)) {
                /* Different types that are subtypes of each other ->
                 * rhs is a subtype of lhs iff lhs.value contains rhs.value.
                 */
                List<Object> lhsValues =
                        AnnotationUtils.getElementValueArray(lhs, "value", Object.class, true);
                List<Object> rhsValues =
                        AnnotationUtils.getElementValueArray(rhs, "value", Object.class, true);
                return rhsValues.containsAll(lhsValues);
            }
            return false;
        }

        // Gives subtyping information but ignores all values.
        private boolean isSubtypeRelaxed(AnnotationMirror rhs, AnnotationMirror lhs) {
            return super.isSubtype(removeValue(rhs), removeValue(lhs));
        }

        // FIXME: #DoBeEvil #NewGoogleMotto #kludge
        // In all seriousness, this probably isn't a good idea but it works.
        // The goal is to be able to tell if in the base hierarchy the two
        // types would be subtypes if they had the same arguments. Lifted
        // from similar evils observed in the old index checker.
        private AnnotationMirror removeValue(AnnotationMirror type) {
            if (AnnotationUtils.areSameIgnoringValues(type, LTL)) {
                return LTL;
            }
            if (AnnotationUtils.areSameIgnoringValues(type, EL)) {
                return EL;
            }
            if (AnnotationUtils.areSameIgnoringValues(type, LTEL)) {
                return LTEL;
            } else {
                return UNKNOWN;
            }
        }
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                new UpperBoundTreeAnnotator(this), new PropagationTreeAnnotator(this));
    }

    protected class UpperBoundTreeAnnotator extends TreeAnnotator {

        public UpperBoundTreeAnnotator(UpperBoundAnnotatedTypeFactory factory) {
            super(factory);
        }

        @Override
        public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
            // I'm not sure we actually care all that much about what's happening here.
            // Maybe a few small rules for addition/subtraction by 0/1, etc. FIXME.
            ExpressionTree left = tree.getLeftOperand();
            ExpressionTree right = tree.getRightOperand();
            switch (tree.getKind()) {
                case PLUS:
                    addAnnotationForPlus(left, right, type);
                    break;
                case MINUS:
                    addAnnotationForMinus(left, right, type);
                    break;
                default:
                    break;
            }
            return super.visitBinary(tree, type);
        }

        private void addAnnotationForLiteralPlus(
                int val, AnnotatedTypeMirror nonLiteralType, AnnotatedTypeMirror type) {
            if (val == 0) {
                type.addAnnotation(nonLiteralType.getAnnotationInHierarchy(LTEL));
                return;
            }
            if (val == 1) {
                if (nonLiteralType.hasAnnotationRelaxed(LTL)) {
                    String[] names =
                            UpperBoundUtils.getValue(nonLiteralType.getAnnotationInHierarchy(LTEL));
                    type.replaceAnnotation(createLessThanOrEqualToLengthAnnotation(names));
                    return;
                }
                type.addAnnotation(UNKNOWN);
                return;
            }
            if (val < 0) {
                if (nonLiteralType.hasAnnotationRelaxed(LTL)
                        || nonLiteralType.hasAnnotationRelaxed(EL)
                        || nonLiteralType.hasAnnotationRelaxed(LTEL)) {

                    String[] names =
                            UpperBoundUtils.getValue(nonLiteralType.getAnnotationInHierarchy(LTEL));
                    type.replaceAnnotation(createLessThanLengthAnnotation(names));
                    return;
                }
                type.addAnnotation(UNKNOWN);
                return;
            }
            // Covers positive numbers.
            type.addAnnotation(UNKNOWN);
            return;
        }

        /** addAnnotationForPlus handles the following cases:
         *  <pre>
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
         *  Implements two rules:
         *  1. If there is a literal on the right side of a subtraction, call our literal add method,
         *     replacing the literal with the literal times negative one.
         *  2. Since EL implies that the number is either positive or zero, subtracting it from
         *     something that's already LTL or EL always implies LTL, and from LTEL implies LTEL.
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
            AnnotatedTypeMirror rightType = getAnnotatedType(rightExpr);
            if (rightType.hasAnnotationRelaxed(EL)) {
                if (leftType.hasAnnotationRelaxed(EL) || leftType.hasAnnotationRelaxed(LTL)) {
                    String[] names =
                            UpperBoundUtils.getValue(leftType.getAnnotationInHierarchy(LTEL));
                    type.replaceAnnotation(createLessThanLengthAnnotation(names));
                    return;
                }
                if (leftType.hasAnnotationRelaxed(LTEL)) {
                    String[] names =
                            UpperBoundUtils.getValue(leftType.getAnnotationInHierarchy(LTEL));
                    type.replaceAnnotation(createLessThanOrEqualToLengthAnnotation(names));
                    return;
                }
            }
            type.addAnnotation(UNKNOWN);
            return;
        }
    }
}
