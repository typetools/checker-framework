package org.checkerframework.checker.samelen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.samelen.qual.SameLen;
import org.checkerframework.checker.samelen.qual.SameLenBottom;
import org.checkerframework.checker.samelen.qual.SameLenUnknown;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ImplicitsTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * The SameLen Checker is used to determine whether there are multiple arrays in a program that
 * share the same length. It is part of the Index Checker, and is used as a subchecker by the Index
 * Checker's components.
 */
public class SameLenAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    AnnotationMirror UNKNOWN;
    private AnnotationMirror BOTTOM;

    public SameLenAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        UNKNOWN = AnnotationUtils.fromClass(elements, SameLenUnknown.class);
        BOTTOM = AnnotationUtils.fromClass(elements, SameLenBottom.class);
        this.postInit();
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new SameLenQualifierHierarchy(factory);
    }

    /**
     * Checks whether the two string lists contain at least one string that's the same. Not a smart
     * algorithm; meant to be run over small sets of data.
     *
     * @param a1 the first string list
     * @param a2 the second string list
     * @return true if there is the intersection is non-empty; false otherwise
     */
    private boolean overlap(List<String> a1, List<String> a2) {
        for (String a : a1) {
            for (String b : a2) {
                if (a.equals(b)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * This function finds the union of the values of two annotations. Both annotations must have a
     * value field; otherwise the function will fail.
     *
     * @return the set union of the two value fields
     */
    private AnnotationMirror getCombinedSameLen(List<String> a1Names, List<String> a2Names) {

        HashSet<String> newValues = new HashSet<String>(a1Names.size() + a2Names.size());

        newValues.addAll(a1Names);
        newValues.addAll(a2Names);
        String[] names = newValues.toArray(new String[0]);
        return createSameLen(names);
    }

    /**
     * For the use of the transfer function; generates a SameLen that includes a and b, as well as
     * everything in sl1 and sl2, if they are SameLen annotations.
     *
     * @param a the name of the first array
     * @param b the name of the second array
     * @param sl1 the current annotation of the first array
     * @param sl2 the current annotation of the second array
     * @return a combined SameLen annotation
     */
    public AnnotationMirror createCombinedSameLen(
            String a, String b, AnnotationMirror sl1, AnnotationMirror sl2) {

        // The names of the arrays.
        ArrayList<String> arrayNames = new ArrayList<>();
        arrayNames.add(a);
        arrayNames.add(b);

        ArrayList<String> slStrings = new ArrayList<>();
        if (AnnotationUtils.areSameByClass(sl1, SameLen.class)) {
            slStrings.addAll(SameLenUtils.getValue(sl1));
        }
        if (AnnotationUtils.areSameByClass(sl2, SameLen.class)) {
            slStrings.addAll(SameLenUtils.getValue(sl2));
        }

        return getCombinedSameLen(arrayNames, slStrings);
    }

    /**
     * The qualifier hierarchy for the sameLen type system. SameLen is strange, because most types
     * are distinct and at the same level: for instance @SameLen("a") and @SameLen("b) have nothing
     * in common. However, if one type includes even one overlapping name, then the types have to be
     * the same: so @SameLen({"a","b","c"} and @SameLen({"c","f","g"} are actually the same type,
     * and have to be treated as such - both should usually be replaced by a SameLen with the union
     * of the lists of names.
     */
    private final class SameLenQualifierHierarchy extends MultiGraphQualifierHierarchy {

        /** @param factory MultiGraphFactory to use to construct this */
        public SameLenQualifierHierarchy(MultiGraphQualifierHierarchy.MultiGraphFactory factory) {
            super(factory);
        }

        @Override
        public AnnotationMirror getTopAnnotation(AnnotationMirror start) {
            return UNKNOWN;
        }

        @Override
        public AnnotationMirror greatestLowerBound(AnnotationMirror a1, AnnotationMirror a2) {
            if (AnnotationUtils.hasElementValue(a1, "value")
                    && AnnotationUtils.hasElementValue(a2, "value")) {
                List<String> a1Val = SameLenUtils.getValue(a1);
                List<String> a2Val = SameLenUtils.getValue(a2);

                if (overlap(a1Val, a2Val)) {
                    return getCombinedSameLen(a1Val, a2Val);
                } else {
                    return BOTTOM;
                }

            } else {
                // the glb is either one of the annotations (if the other is top), or bottom.
                if (AnnotationUtils.areSameByClass(a1, SameLenUnknown.class)) {
                    return a2;
                } else if (AnnotationUtils.areSameByClass(a2, SameLenUnknown.class)) {
                    return a1;
                } else {
                    return BOTTOM;
                }
            }
        }

        @Override
        public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
            if (AnnotationUtils.hasElementValue(a1, "value")
                    && AnnotationUtils.hasElementValue(a2, "value")) {
                List<String> a1Val = SameLenUtils.getValue(a1);
                List<String> a2Val = SameLenUtils.getValue(a2);

                if (overlap(a1Val, a2Val)) {
                    return getCombinedSameLen(a1Val, a2Val);
                } else {
                    return UNKNOWN;
                }

            } else {
                // the lub is either one of the annotations (if the other is bottom), or top.
                if (AnnotationUtils.areSameByClass(a1, SameLenBottom.class)) {
                    return a2;
                } else if (AnnotationUtils.areSameByClass(a2, SameLenBottom.class)) {
                    return a1;
                } else {
                    return UNKNOWN;
                }
            }
        }

        /**
         * Computes subtyping. First checks if one is bottom or the other top (since top is a
         * subtype of nothing, but everything is a subtype of it, and bottom is a subtype of
         * everything, but nothing is a subtype of it. Then, checks if the types are the same. If
         * they are, return true. Otherwise, they're distinct, so return false.
         *
         * @return true if rhs is a subtype of lhs, false otherwise
         */
        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
            if (AnnotationUtils.areSameByClass(rhs, SameLenBottom.class)) {
                return true;
            }
            if (AnnotationUtils.areSameByClass(lhs, SameLenUnknown.class)) {
                return true;
            }
            if (AnnotationUtils.hasElementValue(rhs, "value")
                    && AnnotationUtils.hasElementValue(lhs, "value")) {
                List<String> a1Val = SameLenUtils.getValue(rhs);
                List<String> a2Val = SameLenUtils.getValue(lhs);

                if (overlap(a1Val, a2Val)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                new SameLenTreeAnnotator(this),
                new PropagationTreeAnnotator(this),
                new ImplicitsTreeAnnotator(this));
    }

    protected class SameLenTreeAnnotator extends TreeAnnotator {
        public SameLenTreeAnnotator(SameLenAnnotatedTypeFactory factory) {
            super(factory);
        }
    }

    public AnnotationMirror createSameLen(String... val) {
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, SameLen.class);
        builder.setValue("value", val);
        return builder.build();
    }
}
