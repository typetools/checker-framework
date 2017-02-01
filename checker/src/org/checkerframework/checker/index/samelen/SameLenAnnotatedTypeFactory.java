package org.checkerframework.checker.index.samelen;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.index.qual.SameLen;
import org.checkerframework.checker.index.qual.SameLenBottom;
import org.checkerframework.checker.index.qual.SameLenUnknown;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.framework.util.dependenttypes.DependentTypesHelper;
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
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        // Because the Index Checker is a subclass, the qualifiers have to be explicitly defined.
        return new LinkedHashSet<>(
                Arrays.asList(SameLen.class, SameLenBottom.class, SameLenUnknown.class));
    }

    @Override
    protected DependentTypesHelper createDependentTypesHelper() {
        return new DependentTypesHelper(this, SameLen.class);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new SameLenQualifierHierarchy(factory);
    }

    /**
     * Checks whether the two string lists contain at least one string that's the same. Not a smart
     * algorithm; meant to be run over small sets of data.
     *
     * @param listA the first string list
     * @param listB the second string list
     * @return true if the intersection is non-empty; false otherwise
     */
    private boolean overlap(List<String> listA, List<String> listB) {
        for (String a : listA) {
            for (String b : listB) {
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
    public AnnotationMirror getCombinedSameLen(List<String> a1Names, List<String> a2Names) {

        HashSet<String> newValues = new HashSet<String>(a1Names.size() + a2Names.size());

        newValues.addAll(a1Names);
        newValues.addAll(a2Names);

        String[] names = newValues.toArray(new String[newValues.size()]);
        Arrays.sort(names);
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

        List<String> aValues = new ArrayList<String>();
        aValues.add(a);
        if (AnnotationUtils.areSameByClass(sl1, SameLen.class)) {
            aValues.addAll(SameLenUtils.getValue(sl1));
        }
        List<String> bValues = new ArrayList<String>();
        bValues.add(b);
        if (AnnotationUtils.areSameByClass(sl2, SameLen.class)) {
            bValues.addAll(SameLenUtils.getValue(sl2));
        }

        return getCombinedSameLen(aValues, bValues);
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

    /** Creates a @SameLen annotation whose values are the given strings. */
    public AnnotationMirror createSameLen(String... val) {
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, SameLen.class);
        builder.setValue("value", val);
        return builder.build();
    }
}
