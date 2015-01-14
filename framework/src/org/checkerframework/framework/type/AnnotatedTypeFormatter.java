package org.checkerframework.framework.type;

import org.checkerframework.dataflow.qual.SideEffectFree;

/**
 * Converts an AnnotatedTypeMirror mirror into a formatted string.
 * For converting AnnotationMirrors:
 * @see org.checkerframework.framework.util.AnnotationFormatter
 */
public interface AnnotatedTypeFormatter {
    /**
     * Formats type into a String.  Uses an implementation specific default for
     * printing "invisible annotations"
     * @see org.checkerframework.framework.qual.InvisibleQualifier
     * @param type The type to be converted
     * @return A string representation of type
     */
    @SideEffectFree
    public String format(AnnotatedTypeMirror type);

    /**
     * Formats type into a String.
     * @param type The type to be converted
     * @param printInvisibles whether or not to print invisible annotations
     * @see org.checkerframework.framework.qual.InvisibleQualifier
     * @return A string representation of type
     */
    @SideEffectFree
    public String format(AnnotatedTypeMirror type, boolean printInvisibles);

}
