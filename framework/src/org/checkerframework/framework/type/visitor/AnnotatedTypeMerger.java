package org.checkerframework.framework.type.visitor;

import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.ErrorReporter;

import javax.lang.model.element.AnnotationMirror;

/**
 * Replaces or adds all the annotations in the parameter with the annotations
 * from the visited type. An annotation is replaced if the parameter type
 * already has an annotation in the same hierarchy at the same location as the
 * visited type.
 * 
 * Example use: AnnotatedTypeMirror visitType = ...; AnnotatedTypeMirror
 * parameter - ...; visitType.accept(new AnnotatedTypesMerger(), parameter);
 * 
 * @author smillst
 * 
 */
public class AnnotatedTypeMerger extends AnnotatedTypeComparer<Void> {

    //if top != null we replace only the annotations in the hierarchy of top
    private final AnnotationMirror top;

    public AnnotatedTypeMerger() {
        this.top = null;
    }

    /**
     * @param top if top != null, then only annotation in the hierarchy of top are affected by this merger
     */
    public AnnotatedTypeMerger(final AnnotationMirror top) {
        this.top = top;
    }

    @Override
    protected Void compare(AnnotatedTypeMirror one, AnnotatedTypeMirror two) {
        if (one != null && two != null) {
            replaceAnnotations(one, two);
        }
        return null;
    }

    @Override
    protected Void combineRs(Void r1, Void r2) {
        return r1;
    }

    protected void replaceAnnotations(final AnnotatedTypeMirror one, final AnnotatedTypeMirror two) {
        if(top == null) {
            two.replaceAnnotations(one.getAnnotations());
        } else {
            final AnnotationMirror replacement =  one.getAnnotationInHierarchy(top);
            if(replacement != null) {
                two.replaceAnnotation(one.getAnnotationInHierarchy(top));
            }
        }
    }

    public static void merge(final AnnotatedTypeMirror from, final AnnotatedTypeMirror to) {
        if (from == to) {
            ErrorReporter.errorAbort("From == to");
        }
        new AnnotatedTypeMerger().visit(from, to);
    }

    public static void merge(final AnnotatedTypeMirror from, final AnnotatedTypeMirror to, final AnnotationMirror top) {
        if (from == to) {
            ErrorReporter.errorAbort("From == to");
        }
        new AnnotatedTypeMerger(top).visit(from, to);
    }
}
