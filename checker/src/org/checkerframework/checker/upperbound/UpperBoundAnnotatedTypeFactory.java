package org.checkerframework.checker.upperbound;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;

import org.checkerframework.checker.upperbound.qual.*;

import org.checkerframework.common.basetype.BaseTypeChecker;

import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;

import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;

import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;

import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.Pair;

public class UpperBoundAnnotatedTypeFactory extends
    GenericAnnotatedTypeFactory<CFValue, CFStore, UpperBoundTransfer, UpperBoundAnalysis> {

    public static AnnotationMirror LTL, LTEL, EL, UNKNOWN;
    private final ValueAnnotatedTypeFactory valueAnnotatedTypeFactory;

    protected static ProcessingEnvironment env;

    public UpperBoundAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        LTL = AnnotationUtils.fromClass(elements, LessThanLength.class);
        LTEL = AnnotationUtils.fromClass(elements, LessThanOrEqualToLength.class);
        EL = AnnotationUtils.fromClass(elements, EqualToLength.class);
        UNKNOWN = AnnotationUtils.fromClass(elements, UpperBoundUnknown.class);
        valueAnnotatedTypeFactory = getTypeFactoryOfSubchecker(ValueChecker.class);
        env = checker.getProcessingEnvironment();
        this.postInit();
    }

    @Override
    protected UpperBoundAnalysis createFlowAnalysis(
            List<Pair<VariableElement, CFValue>> fieldValues) {
        return new UpperBoundAnalysis(checker, this, fieldValues);
    }

    /**
     * Creates an annotation of the name given with the set of values given.
     *
     * @return annotation given by name with names=values, or UNKNOWN
     */
    private AnnotationMirror createAnnotation(String name, Set<?> values) {
        if (values.size() > 0) {
            AnnotationBuilder builder = new AnnotationBuilder(env,
                    name);
            List<Object> valuesList = new ArrayList<Object>(values);
            builder.setValue("value", valuesList);
            return builder.build();
        } else {
            return UNKNOWN;
        }
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new UpperBoundQualifierHierarchy(factory);
    }

    /**
     * The qualifier hierarchy for the upperbound type system
     * What does the qh do? When I figure that out I'll write it here
     */
    private final class UpperBoundQualifierHierarchy extends
            MultiGraphQualifierHierarchy {
        /**
         * @param factory
         *            MultiGraphFactory to use to construct this
         */
        public UpperBoundQualifierHierarchy(
                MultiGraphQualifierHierarchy.MultiGraphFactory factory) {
            super(factory);
        }

        @Override
        public AnnotationMirror greatestLowerBound(AnnotationMirror a1,
                AnnotationMirror a2) {
            if (isSubtype(a1, a2)) {
                return a1;
            } else if (isSubtype(a2, a1)) {
                return a2;
            } else {
                /* If the two are unrelated, then the type hierarchy implies
                   that one is LTL and the other is EL, meaning that the GLB
                   is LTEL of all every array that is in either - since LTEL
                   is the bottom type
                */
                List<Object> a1Names = AnnotationUtils.getElementValueArray(
                        a1, "value", Object.class, true);
                List<Object> a2Names = AnnotationUtils.getElementValueArray(
                        a2, "value", Object.class, true);
                HashSet<Object> newValues = new HashSet<Object>(a1Names.size()
                        + a2Names.size());

                newValues.addAll(a1Names);
                newValues.addAll(a2Names);

                return createAnnotation("LTEL",
                        newValues);
            }
        }

        /**
         * Determines the least upper bound of a1 and a2. If a1 and a2 are both
         * the same type of Value annotation, then the LUB is the result of
         * taking all values from both a1 and a2 and removing duplicates.
         *
         * @return the least upper bound of a1 and a2
         */
        @Override
        public AnnotationMirror leastUpperBound(AnnotationMirror a1,
                AnnotationMirror a2) {
            if (!AnnotationUtils.areSameIgnoringValues(getTopAnnotation(a1),
                    getTopAnnotation(a2))) {
                return null;
            } else if (isSubtype(a1, a2)) {
                return a2;
            } else if (isSubtype(a2, a1)) {
                return a1;
            }
            // If both are the same type, determine the type and merge:
            else if (AnnotationUtils.areSameIgnoringValues(a1, a2)) {
                List<Object> a1Values = AnnotationUtils.getElementValueArray(
                        a1, "value", Object.class, true);
                List<Object> a2Values = AnnotationUtils.getElementValueArray(
                        a2, "value", Object.class, true);
                HashSet<Object> newValues = new HashSet<Object>(a1Values.size()
                        + a2Values.size());

                newValues.addAll(a1Values);
                newValues.addAll(a2Values);

                return createAnnotation(a1.getAnnotationType().toString(),
                        newValues);
            }
            // Annotations are in this hierarchy, but they are not the same
            else {
                return UNKNOWN;
            }
        }

        /**
         * Computes subtyping as per the subtyping in the qualifier hierarchy
         * structure unless both annotations are the same. In this case, rhs is a
         * subtype of lhs iff lhs contains at least every element of rhs
         *
         * @return true if rhs is a subtype of lhs, false otherwise
         */
        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {

            if (AnnotationUtils.areSameByClass(lhs, UpperBoundUnknown.class)) {
                return true;
            } else if (AnnotationUtils.areSameByClass(rhs, UpperBoundUnknown.class)) {
                return false;
            } else if (AnnotationUtils.areSameIgnoringValues(lhs, rhs)) {
                // Same type, so might be subtype
                List<Object> lhsValues = AnnotationUtils.getElementValueArray(
                        lhs, "value", Object.class, true);
                List<Object> rhsValues = AnnotationUtils.getElementValueArray(
                        rhs, "value", Object.class, true);
                return rhsValues.containsAll(lhsValues);
            } else if (isSubtypeRelaxed(rhs, lhs)) {
                /* different types that are subtypes of each other ->
                 * rhs is a subtype of lhs iff lhs.value contains rhs.value
                 */
                List<Object> lhsValues = AnnotationUtils.getElementValueArray(
                        lhs, "value", Object.class, true);
                List<Object> rhsValues = AnnotationUtils.getElementValueArray(
                        rhs, "value", Object.class, true);
                return rhsValues.containsAll(lhsValues);
            }
            return false;
        }

        // gives subtyping information but ignores all values
        private boolean isSubtypeRelaxed(AnnotationMirror rhs, AnnotationMirror lhs) {
            return super.isSubtype(removeValue(rhs), removeValue(lhs));
        }
        // #DoBeEvil #NewGoogleMotto #kludge
        private AnnotationMirror removeValue(AnnotationMirror type) {
            if (AnnotationUtils.areSameIgnoringValues(type, LTL)) {
                return LTL;
            }
	    if (AnnotationUtils.areSameIgnoringValues(type, EL)) {
		return EL;
            }
            if (AnnotationUtils.areSameIgnoringValues(type, LTEL)) {
                return LTEL;
            }
            else {
                return UNKNOWN;
            }
        }
    }

    protected class UpperBoundTreeAnnotator extends TreeAnnotator {

        public UpperBoundTreeAnnotator(UpperBoundAnnotatedTypeFactory factory) {
            super(factory);
        }
    }
}
