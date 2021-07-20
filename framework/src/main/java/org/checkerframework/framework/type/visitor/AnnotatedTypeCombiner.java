package org.checkerframework.framework.type.visitor;

import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;

import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

/** Changes each parameter type to be the GLB of the parameter type and visited type. */
public class AnnotatedTypeCombiner extends DoubleAnnotatedTypeScanner<Void> {

    /**
     * Combines all annotations from {@code from} and {@code to} into {@code to} using the GLB.
     *
     * @param from the annotated type mirror from which to take annotations
     * @param to the annotated type mirror into which annotations should be combined
     * @param hierarchy the top type of the hierarchy whose annotations should be combined
     */
    @SuppressWarnings("interning:not.interned") // assertion
    public static void combine(
            final AnnotatedTypeMirror from,
            final AnnotatedTypeMirror to,
            final QualifierHierarchy hierarchy) {
        if (from == to) {
            throw new BugInCF("from == to: %s", from);
        }
        new AnnotatedTypeCombiner(hierarchy).visit(from, to);
    }

    /** The hierarchy used to compute the GLB. */
    private final QualifierHierarchy hierarchy;

    /**
     * Create an AnnotatedTypeCombiner.
     *
     * @param hierarchy the hierarchy used to the compute the GLB
     */
    public AnnotatedTypeCombiner(final QualifierHierarchy hierarchy) {
        this.hierarchy = hierarchy;
    }

    @Override
    @SuppressWarnings("interning:not.interned") // assertion
    protected Void defaultAction(AnnotatedTypeMirror one, AnnotatedTypeMirror two) {
        assert one != two;
        if (one != null && two != null) {
            combineAnnotations(one, two);
        }
        return null;
    }

    /**
     * Computes the greatest lower bound of each set of annotations shared by from and to, and
     * replaces the annotations in to with the result.
     *
     * @param from the first set of annotations
     * @param to the second set of annotations. This is modified by side-effect to hold the result.
     */
    protected void combineAnnotations(
            final AnnotatedTypeMirror from, final AnnotatedTypeMirror to) {

        Set<AnnotationMirror> combinedAnnotations = AnnotationUtils.createAnnotationSet();

        for (AnnotationMirror top : hierarchy.getTopAnnotations()) {
            AnnotationMirror aFrom = from.getAnnotationInHierarchy(top);
            AnnotationMirror aTo = to.getAnnotationInHierarchy(top);
            if (aFrom != null && aTo != null) {
                combinedAnnotations.add(hierarchy.greatestLowerBound(aFrom, aTo));
            } else if (aFrom != null) {
                combinedAnnotations.add(aFrom);
            } else if (aTo != null) {
                combinedAnnotations.add(aTo);
            }
        }
        to.replaceAnnotations(combinedAnnotations);
    }
}
