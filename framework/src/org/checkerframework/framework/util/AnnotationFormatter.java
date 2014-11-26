package org.checkerframework.framework.util;

import org.checkerframework.dataflow.qual.SideEffectFree;

import javax.lang.model.element.AnnotationMirror;
import java.util.Collection;

/**
 * Converts AnnotationMirrors to Strings
 * For converting AnnotatedTypeMirrors:
 * @see org.checkerframework.framework.type.AnnotatedTypeFormatter
 */
public interface AnnotationFormatter {

    /**
     * Converts a collection of annotation mirrors into a String
     * @param annos A collection of annotations to print
     * @param printInvisible Whether or not to print "invisible" annotation mirrors
     * @see org.checkerframework.framework.qual.InvisibleQualifier
     * @return A string representation of annos
     */
    @SideEffectFree
    public String formatAnnotationString(Collection<? extends AnnotationMirror> annos, boolean printInvisible);

    /**
     * Converts an individual annotation mirror into a String
     * @param anno The annotation mirror to convert
     * @return A String representation of anno
     */
    @SideEffectFree
    public String formatAnnotationMirror(AnnotationMirror anno);
}
