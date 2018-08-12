package org.checkerframework.checker.index.substringindex;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.index.OffsetDependentTypesHelper;
import org.checkerframework.checker.index.qual.SubstringIndexBottom;
import org.checkerframework.checker.index.qual.SubstringIndexFor;
import org.checkerframework.checker.index.qual.SubstringIndexUnknown;
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
 * Builds types with annotations from the Substring Index checker hierarchy, which contains
 * the @{@link SubstringIndexFor} annotation.
 */
public class SubstringIndexAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /** The top qualifier of the Substring Index hierarchy. */
    public final AnnotationMirror UNKNOWN;
    /** The bottom qualifier of the Substring Index hierarchy. */
    public final AnnotationMirror BOTTOM;

    public SubstringIndexAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        UNKNOWN = AnnotationBuilder.fromClass(elements, SubstringIndexUnknown.class);
        BOTTOM = AnnotationBuilder.fromClass(elements, SubstringIndexBottom.class);
        this.postInit();
    }

    /**
     * Returns a mutable set of annotation classes that are supported by the Substring Index
     * Checker.
     *
     * @return mutable set containing annotation classes from the Substring Index qualifier
     *     hierarchy
     */
    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new LinkedHashSet<>(
                Arrays.asList(
                        SubstringIndexUnknown.class,
                        SubstringIndexFor.class,
                        SubstringIndexBottom.class));
    }

    /** Creates the Substring Index qualifier hierarchy. */
    @Override
    public QualifierHierarchy createQualifierHierarchy(
            MultiGraphQualifierHierarchy.MultiGraphFactory factory) {
        return new SubstringIndexQualifierHierarchy(factory);
    }

    /**
     * Creates an {@link DependentTypesHelper} that allows use of addition and subtraction in the
     * Substring Index Checker annotations.
     */
    @Override
    protected DependentTypesHelper createDependentTypesHelper() {
        return new OffsetDependentTypesHelper(this);
    }

    /**
     * The Substring Index qualifier hierarchy. The hierarchy consists of a top element {@link
     * UNKNOWN} of type {@link SubstringIndexUnknown}, bottom element {@link BOTTOM} of type {@link
     * SubstringIndexBottom}, and elements of type {@link SubstringIndexFor} that follow the
     * subtyping relation of {@link UBQualifier}.
     */
    private final class SubstringIndexQualifierHierarchy extends MultiGraphQualifierHierarchy {

        public SubstringIndexQualifierHierarchy(
                MultiGraphQualifierHierarchy.MultiGraphFactory factory) {
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
            UBQualifier ubq1 = UBQualifier.createUBQualifier(a1);
            UBQualifier ubq2 = UBQualifier.createUBQualifier(a2);
            UBQualifier glb = ubq1.glb(ubq2);
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
            UBQualifier ubq1 = UBQualifier.createUBQualifier(a1);
            UBQualifier ubq2 = UBQualifier.createUBQualifier(a2);
            UBQualifier lub = ubq1.lub(ubq2);
            return convertUBQualifierToAnnotation(lub);
        }

        @Override
        public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {
            if (AnnotationUtils.areSameByClass(superAnno, SubstringIndexUnknown.class)) {
                return true;
            }
            if (AnnotationUtils.areSameByClass(subAnno, SubstringIndexBottom.class)) {
                return true;
            }
            if (AnnotationUtils.areSameByClass(subAnno, SubstringIndexUnknown.class)) {
                return false;
            }
            if (AnnotationUtils.areSameByClass(superAnno, SubstringIndexBottom.class)) {
                return false;
            }

            UBQualifier subtype = UBQualifier.createUBQualifier(subAnno);
            UBQualifier supertype = UBQualifier.createUBQualifier(superAnno);
            return subtype.isSubtype(supertype);
        }
    }

    /**
     * Converts an instance of {@link UBQualifier} to an annotation from the Substring Index
     * hierarchy.
     *
     * @param qualifier the {@link UBQualifier} to be converted
     * @return an annotation from the Substring Index hierarchy, representing {@code qualifier}
     */
    public AnnotationMirror convertUBQualifierToAnnotation(UBQualifier qualifier) {
        if (qualifier.isUnknown()) {
            return UNKNOWN;
        } else if (qualifier.isBottom()) {
            return BOTTOM;
        }

        LessThanLengthOf ltlQualifier = (LessThanLengthOf) qualifier;
        return ltlQualifier.convertToSubstringIndexAnnotation(processingEnv);
    }
}
