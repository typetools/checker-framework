package org.checkerframework.framework.type;

import org.checkerframework.dataflow.qual.SideEffectFree;

/**
 * Converts an AnnotatedTypeMirror mirror into a formatted string. For converting AnnotationMirrors:
 *
 * @see org.checkerframework.framework.util.AnnotationFormatter
 */
public interface AnnotatedTypeFormatter {
    /**
     * Formats type into a String. Uses an implementation specific default for printing "invisible
     * annotations"
     *
     * @see org.checkerframework.framework.qual.InvisibleQualifier
     * @param type the type to be converted
     * @return a string representation of type
     */
    @SideEffectFree
    public String format(AnnotatedTypeMirror type);

    /**
     * Formats type into a String.
     *
     * @param type the type to be converted
     * @param printVerbose whether or not to print verbosely
     * @see org.checkerframework.framework.qual.InvisibleQualifier
     * @return a string representation of type
     */
    @SideEffectFree
    public String format(AnnotatedTypeMirror type, boolean printVerbose);
}
