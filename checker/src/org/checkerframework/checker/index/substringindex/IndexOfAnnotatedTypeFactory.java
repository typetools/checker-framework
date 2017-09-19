package org.checkerframework.checker.index.substringindex;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.index.OffsetDependentTypesHelper;
import org.checkerframework.checker.index.qual.IndexOfBottom;
import org.checkerframework.checker.index.qual.IndexOfIndexFor;
import org.checkerframework.checker.index.qual.IndexOfUnknown;
import org.checkerframework.checker.index.upperbound.UBQualifier;
import org.checkerframework.checker.index.upperbound.UBQualifier.LessThanLengthOf;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.dependenttypes.DependentTypesHelper;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * Builds types with annotations from the IndexOf checker hierarchy, which contains the @{@link
 * IndexOfIndexFor} annotation. This annotation is used to annotate the return value of {@link
 * java.lang.String#indexOf(String) String.indexOf} and {@link java.lang.String#lastIndexOf(String)
 * String.lastIndexOf} and allow the Upper Bound Checker to infer @{@link
 * org.checkerframework.checker.index.qual.LTLengthOf} annotations with the same parameters for
 * expressions that are known by the index checker to be non-negative.
 */
public class IndexOfAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /** The top qualifier of the IndexOf hierarchy */
    public final AnnotationMirror UNKNOWN;
    /** The bottom qualifier of the IndexOf hierarchy */
    public final AnnotationMirror BOTTOM;

    public IndexOfAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        UNKNOWN = AnnotationBuilder.fromClass(elements, IndexOfUnknown.class);
        BOTTOM = AnnotationBuilder.fromClass(elements, IndexOfBottom.class);
        this.postInit();
    }

    /**
     * Returns a mutable set of annotation classes that are supported by the IndexOf Checker.
     *
     * @return mutable set containing annotation classes from the IndexOf qualifier hierarchy
     */
    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new LinkedHashSet<>(
                Arrays.asList(IndexOfBottom.class, IndexOfUnknown.class, IndexOfIndexFor.class));
    }

    /** Creates the IndexOf qualifier hierarchy. */
    @Override
    public QualifierHierarchy createQualifierHierarchy(
            MultiGraphQualifierHierarchy.MultiGraphFactory factory) {
        return new IndexOfQualifierHierarchy(factory);
    }

    /**
     * Creates an {@link DependentTypesHelper} that allows use of addition and subtraction in the
     * IndexOf Checker annotations.
     */
    @Override
    protected DependentTypesHelper createDependentTypesHelper() {
        return new OffsetDependentTypesHelper(this);
    }

    /**
     * The IndexOf qualifier hierarchy. The hierarchy consists of a top element {@link UNKNOWN} of
     * type {@link IndexOfUnknown}, bottom element {@link BOTTOM} of type {@link IndexOfBottom}, and
     * elements of type {@link IndexOfIndexFor} that follow the subtyping relation of {@link
     * UBQualifier}.
     */
    private final class IndexOfQualifierHierarchy extends MultiGraphQualifierHierarchy {

        public IndexOfQualifierHierarchy(MultiGraphQualifierHierarchy.MultiGraphFactory factory) {
            super(factory);
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
            UBQualifier a1Obj = UBQualifier.createUBQualifier(a1);
            UBQualifier a2Obj = UBQualifier.createUBQualifier(a2);
            UBQualifier glb = a1Obj.glb(a2Obj);
            return convertUBQualifierToAnnotation(glb);
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
            UBQualifier a1Obj = UBQualifier.createUBQualifier(a1);
            UBQualifier a2Obj = UBQualifier.createUBQualifier(a2);
            UBQualifier lub = a1Obj.lub(a2Obj);
            return convertUBQualifierToAnnotation(lub);
        }

        @Override
        public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {
            if (AnnotationUtils.areSameByClass(superAnno, IndexOfUnknown.class)) {
                return true;
            }
            if (AnnotationUtils.areSameByClass(subAnno, IndexOfBottom.class)) {
                return true;
            }
            if (AnnotationUtils.areSameByClass(subAnno, IndexOfUnknown.class)) {
                return false;
            }
            if (AnnotationUtils.areSameByClass(superAnno, IndexOfBottom.class)) {
                return false;
            }

            UBQualifier subtype = UBQualifier.createUBQualifier(subAnno);
            UBQualifier supertype = UBQualifier.createUBQualifier(superAnno);
            return subtype.isSubtype(supertype);
        }
    }

    /**
     * Converts an instance of {@link UBQualifier} to an annotation from the IndexOf hierarchy.
     *
     * @param qualifier the {@link UBQualifier} to be converted
     * @return an annotation from the IndexOf hierarchy, representing {@code qualifier}
     */
    public AnnotationMirror convertUBQualifierToAnnotation(UBQualifier qualifier) {
        if (qualifier.isUnknown()) {
            return UNKNOWN;
        } else if (qualifier.isBottom()) {
            return BOTTOM;
        }

        LessThanLengthOf ltlQualifier = (LessThanLengthOf) qualifier;
        return ltlQualifier.convertToIndexOfAnnotation(processingEnv);
    }
}
