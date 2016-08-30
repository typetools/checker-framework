package org.checkerframework.checker.upperbound;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;
import java.util.ArrayList;
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
 * Works primarily by way of querying the minlen checker
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
     *  The field is public so that it can be accessed from the UpperBoundVisitor
     *  when comparing index values against the known minimum lengths of arrays.
     */
    public final ValueAnnotatedTypeFactory valueAnnotatedTypeFactory;

    /**
     *  Provides a way to query the Min Len (minimum length) Checker,
     *  which computes the lengths of arrays.
     */
    private final MinLenAnnotatedTypeFactory minlenAnnotatedTypeFactory;

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
        minlenAnnotatedTypeFactory = getTypeFactoryOfSubchecker(MinLenChecker.class);
        env = checker.getProcessingEnvironment();
        this.postInit();
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

    @Override
    protected UpperBoundAnalysis createFlowAnalysis(
            List<Pair<VariableElement, CFValue>> fieldValues) {
        return new UpperBoundAnalysis(checker, this, fieldValues);
    }

    /**
     * Creates an annotation of the name given with the set of values given.
     * Exists in place of a series of createXAnnotation methods because that
     * would be silly.
     *
     * @return annotation given by name with names=values, or UNKNOWN
     */
    private AnnotationMirror createAnnotation(String name, Set<?> values) {
        if (values.size() > 0) {
            AnnotationBuilder builder = new AnnotationBuilder(env, name);
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

                return createAnnotation("LTEL", newValues);
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
            return super.visitBinary(tree, type);
        }
    }
}
