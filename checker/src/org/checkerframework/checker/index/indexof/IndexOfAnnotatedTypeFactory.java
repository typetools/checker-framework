package org.checkerframework.checker.index.indexof;

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
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * The IndexOf Checker is used to help type the results of calls to the JDK's substring search
 * methods. It is part of the Index Checker.
 */
public class IndexOfAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    public final AnnotationMirror UNKNOWN, BOTTOM;

    public IndexOfAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        UNKNOWN = AnnotationUtils.fromClass(elements, IndexOfUnknown.class);
        BOTTOM = AnnotationUtils.fromClass(elements, IndexOfBottom.class);
        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new LinkedHashSet<>(
                Arrays.asList(IndexOfBottom.class, IndexOfUnknown.class, IndexOfIndexFor.class));
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(
            MultiGraphQualifierHierarchy.MultiGraphFactory factory) {
        return new IndexOfQualifierHierarchy(factory);
    }

    @Override
    protected DependentTypesHelper createDependentTypesHelper() {
        return new OffsetDependentTypesHelper(this);
    }

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
