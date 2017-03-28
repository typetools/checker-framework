package org.checkerframework.checker.index.searchindex;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.index.IndexUtil;
import org.checkerframework.checker.index.qual.NegativeIndexFor;
import org.checkerframework.checker.index.qual.SearchIndex;
import org.checkerframework.checker.index.qual.SearchIndexBottom;
import org.checkerframework.checker.index.qual.SearchIndexUnknown;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * The Search Index Checker is used to help type the results of calls to the JDK's binary search
 * methods. It is part of the Index Checker.
 */
public class SearchIndexAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    public final AnnotationMirror UNKNOWN, BOTTOM;

    public SearchIndexAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        UNKNOWN = AnnotationUtils.fromClass(elements, SearchIndexUnknown.class);
        BOTTOM = AnnotationUtils.fromClass(elements, SearchIndexBottom.class);
        this.postInit();
    }

    /**
     * Provides a way to query the Constant Value Checker, which computes the values of expressions
     * known at compile time (constant prop + folding).
     */
    ValueAnnotatedTypeFactory getValueAnnotatedTypeFactory() {
        return getTypeFactoryOfSubchecker(ValueChecker.class);
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new LinkedHashSet<>(
                Arrays.asList(
                        SearchIndex.class,
                        SearchIndexBottom.class,
                        SearchIndexUnknown.class,
                        NegativeIndexFor.class));
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(
            MultiGraphQualifierHierarchy.MultiGraphFactory factory) {
        return new SearchIndexQualifierHierarchy(factory);
    }

    private final class SearchIndexQualifierHierarchy extends MultiGraphQualifierHierarchy {

        public SearchIndexQualifierHierarchy(
                MultiGraphQualifierHierarchy.MultiGraphFactory factory) {
            super(factory);
        }

        @Override
        public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {

            System.out.println(subAnno);
            System.out.println(superAnno);

            if (AnnotationUtils.areSameByClass(superAnno, SearchIndexUnknown.class)) {
                return true;
            }
            if (AnnotationUtils.areSameByClass(subAnno, SearchIndexBottom.class)) {
                return true;
            }
            if (AnnotationUtils.areSameByClass(subAnno, SearchIndexUnknown.class)) {
                return false;
            }
            if (AnnotationUtils.areSameByClass(superAnno, SearchIndexBottom.class)) {
                return false;
            }
            // All annotations have arguments, because each is either NegativeIndexFor or
            // SearchIndex. All NegativeIndexFor annotations are subtypes of SearchIndex
            // annotations whose arguments contain their arguments.
            if (AnnotationUtils.areSameByClass(subAnno, NegativeIndexFor.class)) {
                List<String> superArrays =
                        IndexUtil.getValueOfAnnotationWithStringArgument(superAnno);
                List<String> subArrays = IndexUtil.getValueOfAnnotationWithStringArgument(subAnno);
                return superArrays != null
                        && subArrays != null
                        && superArrays.containsAll(subArrays);
            }
            if (AnnotationUtils.areSameByClass(subAnno, SearchIndex.class)) {
                List<String> superArrays =
                        IndexUtil.getValueOfAnnotationWithStringArgument(superAnno);
                List<String> subArrays = IndexUtil.getValueOfAnnotationWithStringArgument(subAnno);
                return superArrays != null
                        && subArrays != null
                        && AnnotationUtils.areSameByClass(superAnno, SearchIndex.class)
                        && superArrays.containsAll(subArrays);
            }
            assert false;
            return false; // dead code
        }
    }

    /** Creates a new negative index for annotation with the given arrays as its arguments. */
    AnnotationMirror createNegativeIndexFor(List<String> arrays) {
        if (arrays.size() == 0) {
            return UNKNOWN;
        }
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, NegativeIndexFor.class);
        builder.setValue("value", arrays);
        return builder.build();
    }
}
