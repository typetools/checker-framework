package org.checkerframework.framework.util;

import org.checkerframework.dataflow.qual.SideEffectFree;

import java.util.Collection;

import javax.lang.model.element.AnnotationMirror;

/** Converts AnnotationMirrors to Strings. Used when converting AnnotatedTypeMirrors to Strings. */
public interface AnnotationFormatter {

    /**
     * Converts a collection of annotation mirrors into a String.
     *
     * @param annos a collection of annotations to print
     * @param printInvisible whether or not to print "invisible" annotation mirrors
     * @see org.checkerframework.framework.qual.InvisibleQualifier
     * @return a string representation of annos
     */
    @SideEffectFree
    public String formatAnnotationString(
            Collection<? extends AnnotationMirror> annos, boolean printInvisible);

    /**
     * Converts an individual annotation mirror into a String.
     *
     * @param anno the annotation mirror to convert
     * @return a String representation of anno
     */
    @SideEffectFree
    public String formatAnnotationMirror(AnnotationMirror anno);
}
