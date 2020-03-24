package org.checkerframework.framework.type.visitor;

import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;

/**
 * Combines all the annotations in the parameter with the annotations from the visited type, taking
 * the greatest lower bound at each point.
 */
public class AnnotatedTypeCombiner extends AnnotatedTypeComparer<Void> {

    /**
     * Combines all annotations from {@code from} and {@code to} into {@code to} using the GLB.
     *
     * @param from the annotated type mirror from which to take annotations
     * @param to the annotated type mirror into which annotations should be combined
     * @param hierarchy the top type of the hierarchy whose annotations should be combined
     */
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
     * Private constructor.
     *
     * @param hierarchy the hierarchy used to the compute the GLB
     */
    private AnnotatedTypeCombiner(final QualifierHierarchy hierarchy) {
        this.hierarchy = hierarchy;
    }

    @Override
    protected Void compare(AnnotatedTypeMirror one, AnnotatedTypeMirror two) {
        assert one != two;
        if (one != null && two != null) {
            combineAnnotations(one, two);
        }
        return null;
    }

    @Override
    protected Void combineRs(Void r1, Void r2) {
        return r1;
    }

    /**
     * Computes the greatest lower bound of each set of annotations shared by one and two, and
     * replaces the annotations in two with the result.
     *
     * @param one the first set of annotations
     * @param two the second set of annotations. This is modified by side-effect to hold the result.
     */
    protected void combineAnnotations(
            final AnnotatedTypeMirror one, final AnnotatedTypeMirror two) {

        Set<AnnotationMirror> combinedAnnotations = AnnotationUtils.createAnnotationSet();

        for (AnnotationMirror top : hierarchy.getTopAnnotations()) {
            AnnotationMirror aOne = one.getAnnotationInHierarchy(top);
            AnnotationMirror aTwo = two.getAnnotationInHierarchy(top);
            if (aOne != null && aTwo != null) {
                combinedAnnotations.add(hierarchy.greatestLowerBound(aOne, aTwo));
            } else if (aOne != null) {
                combinedAnnotations.add(aOne);
            } else if (aTwo != null) {
                combinedAnnotations.add(aTwo);
            }
        }
        two.replaceAnnotations(combinedAnnotations);
    }
}
