package org.checkerframework.checker.samelen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.samelen.qual.SameLen;
import org.checkerframework.checker.samelen.qual.SameLenBottom;
import org.checkerframework.checker.samelen.qual.SameLenUnknown;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.ValueChecker;
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

    protected static ProcessingEnvironment env;

    public static AnnotationMirror UNKNOWN;

    /**
     * Provides a way to query the Constant Value Checker, which computes the values of expressions
     * known at compile time (constant prop + folding).
     */
    private final ValueAnnotatedTypeFactory valueAnnotatedTypeFactory;

    public SameLenAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        valueAnnotatedTypeFactory = getTypeFactoryOfSubchecker(ValueChecker.class);
        env = checker.getProcessingEnvironment();
        UNKNOWN = AnnotationUtils.fromClass(elements, SameLenUnknown.class);
        this.postInit();
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new SameLenQualifierHierarchy(factory);
    }

    /**
     * Checks whether the two string arrays contain at least one string that's the same. Not a smart
     * algorithm; meant to be run over small sets of data.
     *
     * @param a1 the first string array
     * @param a2 the second string array
     * @return true if there is the intersection is non-empty; false otherwise
     */
    private boolean overlap(String[] a1, String[] a2) {
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
     * @param a1 an annotation with a value field
     * @param a2 an annotation with a value field
     * @return the set union of the two value fields
     */
    private AnnotationMirror getCombinedSameLen(String[] a1, String[] a2) {

        List<String> a1Names = new ArrayList<>();
        List<String> a2Names = new ArrayList<>();

        for (String s : a1) {
            a1Names.add(s);
        }
        for (String s : a2) {
            a2Names.add(s);
        }

        HashSet<String> newValues = new HashSet<String>(a1Names.size() + a2Names.size());

        newValues.addAll(a1Names);
        newValues.addAll(a2Names);
        Object[] values = newValues.toArray();
        String[] names = new String[values.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = values[i].toString();
        }
        return createSameLen(names);
    }

    /**
     * For the use of the transfer function; generates a SameLen that includes a and b, as well as
     * everything in sl1 and sl2, if they are SameLen annotations.
     *
     * @param a The name of the first array.
     * @param b The name of the second array.
     * @param sl1 The current annotation of the first array.
     * @param sl2 The current annotation of the second array.
     * @return A combined SameLen annotation.
     */
    public AnnotationMirror createCombinedSameLen(
            String a, String b, AnnotationMirror sl1, AnnotationMirror sl2) {
        String[] a1 = {a, b};
        ArrayList<String> slStrings = new ArrayList<>();
        if (AnnotationUtils.areSameByClass(sl1, SameLen.class)) {
            for (String s : SameLenUtils.getValue(sl1)) {
                slStrings.add(s);
            }
        }
        if (AnnotationUtils.areSameByClass(sl2, SameLen.class)) {
            for (String s : SameLenUtils.getValue(sl2)) {
                slStrings.add(s);
            }
        }

        String[] names = new String[slStrings.size()];
        for (int i = 0; i < slStrings.size(); i++) {
            names[i] = slStrings.get(i);
        }

        return getCombinedSameLen(a1, names);
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
            return createSameLenUnknown();
        }

        @Override
        public AnnotationMirror greatestLowerBound(AnnotationMirror a1, AnnotationMirror a2) {
            if (AnnotationUtils.hasElementValue(a1, "value")
                    && AnnotationUtils.hasElementValue(a2, "value")) {
                String[] a1Val = SameLenUtils.getValue(a1);
                String[] a2Val = SameLenUtils.getValue(a2);

                if (overlap(a1Val, a2Val)) {
                    return getCombinedSameLen(a1Val, a2Val);
                } else {
                    return createSameLenBottom();
                }

            } else {
                // the glb is either one of the annotations (if the other is top), or bottom.
                if (AnnotationUtils.areSameByClass(a1, SameLenUnknown.class)) {
                    return a2;
                } else if (AnnotationUtils.areSameByClass(a2, SameLenUnknown.class)) {
                    return a1;
                } else {
                    return createSameLenBottom();
                }
            }
        }

        @Override
        public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
            if (AnnotationUtils.hasElementValue(a1, "value")
                    && AnnotationUtils.hasElementValue(a2, "value")) {
                String[] a1Val = SameLenUtils.getValue(a1);
                String[] a2Val = SameLenUtils.getValue(a2);

                if (overlap(a1Val, a2Val)) {
                    return getCombinedSameLen(a1Val, a2Val);
                } else {
                    return createSameLenUnknown();
                }

            } else {
                // the glb is either one of the annotations (if the other is top), or bottom.
                if (AnnotationUtils.areSameByClass(a1, SameLenUnknown.class)) {
                    return a2;
                } else if (AnnotationUtils.areSameByClass(a2, SameLenUnknown.class)) {
                    return a1;
                } else {
                    return createSameLenUnknown();
                }
            }
        }

        /**
         * Computes subtyping. First checks if one is bottom or the other top (since top is a
         * subtype of nothing, but everything is a subtype of it, and bottom is a subtype of
         * everything, but nothing is a subtype of it. Then, checks if the types are the same. If
         * they are, return true. Otherwise, they're distinct, so return false.
         *
         * @return true if rhs is a subtype of lhs, false otherwise.
         */
        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
            if (AnnotationUtils.areSameByClass(lhs, SameLenBottom.class)) {
                return true;
            }
            if (AnnotationUtils.areSameByClass(rhs, SameLenUnknown.class)) {
                return true;
            }
            if (AnnotationUtils.hasElementValue(rhs, "value")
                    && AnnotationUtils.hasElementValue(lhs, "value")) {
                String[] a1Val = SameLenUtils.getValue(rhs);
                String[] a2Val = SameLenUtils.getValue(lhs);

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

    public AnnotationMirror createSameLen(String val) {
        String[] vals = {val};
        return createSameLen(vals);
    }

    public AnnotationMirror createSameLen(String[] val) {
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, SameLen.class);
        builder.setValue("value", val);
        return builder.build();
    }

    public AnnotationMirror createSameLenBottom() {
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, SameLenBottom.class);
        return builder.build();
    }

    public AnnotationMirror createSameLenUnknown() {
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, SameLenUnknown.class);
        return builder.build();
    }
}
