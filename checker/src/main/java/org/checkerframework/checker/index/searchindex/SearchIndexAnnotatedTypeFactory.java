package org.checkerframework.checker.index.searchindex;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.index.qual.NegativeIndexFor;
import org.checkerframework.checker.index.qual.SearchIndexBottom;
import org.checkerframework.checker.index.qual.SearchIndexFor;
import org.checkerframework.checker.index.qual.SearchIndexUnknown;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.common.value.ValueCheckerUtils;
import org.checkerframework.framework.type.ElementQualifierHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * The Search Index Checker is used to help type the results of calls to the JDK's binary search
 * methods. It is part of the Index Checker.
 */
public class SearchIndexAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /** The @{@link SearchIndexUnknown} annotation. */
    public final AnnotationMirror UNKNOWN =
            AnnotationBuilder.fromClass(elements, SearchIndexUnknown.class);
    /** The @{@link SearchIndexBottom} annotation. */
    public final AnnotationMirror BOTTOM =
            AnnotationBuilder.fromClass(elements, SearchIndexBottom.class);

    /** Create a new SearchIndexAnnotatedTypeFactory. */
    public SearchIndexAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        this.postInit();
    }

    /**
     * Provides a way to query the Constant Value Checker, which computes the values of expressions
     * known at compile time (constant propagation and folding).
     */
    ValueAnnotatedTypeFactory getValueAnnotatedTypeFactory() {
        return getTypeFactoryOfSubchecker(ValueChecker.class);
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new LinkedHashSet<>(
                Arrays.asList(
                        SearchIndexFor.class,
                        SearchIndexBottom.class,
                        SearchIndexUnknown.class,
                        NegativeIndexFor.class));
    }

    @Override
    protected QualifierHierarchy createQualifierHierarchy() {
        return new SearchIndexQualifierHierarchy(this.getSupportedTypeQualifiers(), elements);
    }

    /** SearchIndexQualifierHierarchy */
    private final class SearchIndexQualifierHierarchy extends ElementQualifierHierarchy {

        /**
         * Creates a SearchIndexQualifierHierarchy from the given classes.
         *
         * @param qualifierClasses classes of annotations that are the qualifiers
         * @param elements element utils
         */
        public SearchIndexQualifierHierarchy(
                Set<Class<? extends Annotation>> qualifierClasses, Elements elements) {
            super(qualifierClasses, elements);
        }

        @Override
        public AnnotationMirror greatestLowerBound(AnnotationMirror a1, AnnotationMirror a2) {
            if (AnnotationUtils.areSame(a1, UNKNOWN)) {
                return a2;
            }
            if (AnnotationUtils.areSame(a2, UNKNOWN)) {
                return a1;
            }
            if (AnnotationUtils.areSame(a1, BOTTOM)) {
                return a1;
            }
            if (AnnotationUtils.areSame(a2, BOTTOM)) {
                return a2;
            }
            if (isSubtype(a1, a2)) {
                return a1;
            }
            if (isSubtype(a2, a1)) {
                return a2;
            }
            // If neither is a subtype of the other, then create an
            // annotation that combines their values.

            // Each annotation is either NegativeIndexFor or SearchIndexFor.
            Set<String> combinedArrays =
                    new HashSet<>(ValueCheckerUtils.getValueOfAnnotationWithStringArgument(a1));
            combinedArrays.addAll(ValueCheckerUtils.getValueOfAnnotationWithStringArgument(a2));

            if (areSameByClass(a1, NegativeIndexFor.class)
                    || areSameByClass(a2, NegativeIndexFor.class)) {
                return createNegativeIndexFor(Arrays.asList(combinedArrays.toArray(new String[0])));
            } else {
                return createSearchIndexFor(Arrays.asList(combinedArrays.toArray(new String[0])));
            }
        }

        @Override
        public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
            if (AnnotationUtils.areSame(a1, UNKNOWN)) {
                return a1;
            }
            if (AnnotationUtils.areSame(a2, UNKNOWN)) {
                return a2;
            }
            if (AnnotationUtils.areSame(a1, BOTTOM)) {
                return a2;
            }
            if (AnnotationUtils.areSame(a2, BOTTOM)) {
                return a1;
            }
            if (isSubtype(a1, a2)) {
                return a2;
            }
            if (isSubtype(a2, a1)) {
                return a1;
            }
            // If neither is a subtype of the other, then create an
            // annotation that includes only their overlapping values.

            // Each annotation is either NegativeIndexFor or SearchIndexFor.
            List<String> arrayIntersection =
                    ValueCheckerUtils.getValueOfAnnotationWithStringArgument(a1);
            arrayIntersection.retainAll(
                    ValueCheckerUtils.getValueOfAnnotationWithStringArgument(a2));

            if (arrayIntersection.isEmpty()) {
                return UNKNOWN;
            }

            if (areSameByClass(a1, SearchIndexFor.class)
                    || areSameByClass(a2, SearchIndexFor.class)) {
                return createSearchIndexFor(
                        Arrays.asList(arrayIntersection.toArray(new String[0])));
            } else {
                return createNegativeIndexFor(
                        Arrays.asList(arrayIntersection.toArray(new String[0])));
            }
        }

        @Override
        public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {
            if (areSameByClass(superAnno, SearchIndexUnknown.class)) {
                return true;
            }
            if (areSameByClass(subAnno, SearchIndexBottom.class)) {
                return true;
            }
            if (areSameByClass(subAnno, SearchIndexUnknown.class)) {
                return false;
            }
            if (areSameByClass(superAnno, SearchIndexBottom.class)) {
                return false;
            }

            // Each annotation is either NegativeIndexFor or SearchIndexFor.
            List<String> superArrays =
                    ValueCheckerUtils.getValueOfAnnotationWithStringArgument(superAnno);
            List<String> subArrays =
                    ValueCheckerUtils.getValueOfAnnotationWithStringArgument(subAnno);

            // Subtyping requires:
            //  * subtype is NegativeIndexFor or supertype is SearchIndexFor
            //  * subtype's arrays are a superset of supertype's arrays
            return ((areSameByClass(subAnno, NegativeIndexFor.class)
                            || areSameByClass(superAnno, SearchIndexFor.class))
                    && subArrays.containsAll(superArrays));
        }
    }

    /** Create a new {@code @NegativeIndexFor} annotation with the given arrays as its arguments. */
    AnnotationMirror createNegativeIndexFor(List<String> arrays) {
        if (arrays.isEmpty()) {
            return UNKNOWN;
        }

        arrays = new ArrayList<>(new HashSet<>(arrays)); // remove duplicates
        Collections.sort(arrays);

        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, NegativeIndexFor.class);
        builder.setValue("value", arrays);
        return builder.build();
    }

    /** Create a new {@code @SearchIndexFor} annotation with the given arrays as its arguments. */
    AnnotationMirror createSearchIndexFor(List<String> arrays) {
        if (arrays.isEmpty()) {
            return UNKNOWN;
        }

        arrays = new ArrayList<>(new HashSet<>(arrays)); // remove duplicates
        Collections.sort(arrays);

        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, SearchIndexFor.class);
        builder.setValue("value", arrays);
        return builder.build();
    }
}
