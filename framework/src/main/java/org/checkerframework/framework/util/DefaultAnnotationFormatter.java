package org.checkerframework.framework.util;

import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;

import java.util.Collection;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;

/** A utility for converting AnnotationMirrors to Strings. It omits full package names. */
public class DefaultAnnotationFormatter implements AnnotationFormatter {

    /**
     * Returns true if, by default, anno should not be printed.
     *
     * @see org.checkerframework.framework.qual.InvisibleQualifier
     * @param anno the annotation mirror to test
     * @return true if anno's declaration was qualified by InvisibleQualifier
     */
    public static boolean isInvisibleQualified(AnnotationMirror anno) {
        TypeElement annoElement = (TypeElement) anno.getAnnotationType().asElement();
        return annoElement.getAnnotation(InvisibleQualifier.class) != null;
    }

    /**
     * Creates a String of each annotation in annos separated by a single space character and
     * terminated by a space character, obeying the printInvisible parameter.
     *
     * @param annos a collection of annotations to print
     * @param printInvisible whether or not to print "invisible" annotation mirrors
     * @return the list of annotations converted to a String
     */
    @Override
    @SideEffectFree
    public String formatAnnotationString(
            Collection<? extends AnnotationMirror> annos, boolean printInvisible) {
        StringBuilder sb = new StringBuilder();
        for (AnnotationMirror obj : annos) {
            if (obj == null) {
                throw new BugInCF(
                        "AnnotatedTypeMirror.formatAnnotationString: found null AnnotationMirror");
            }
            if (isInvisibleQualified(obj) && !printInvisible) {
                continue;
            }
            AnnotationUtils.toStringSimple(obj, sb);
            sb.append(" ");
        }
        return sb.toString();
    }

    /**
     * Returns the string representation of a single AnnotationMirror, without showing full package
     * names.
     *
     * @param anno the annotation mirror to convert
     * @return the string representation of a single AnnotationMirror, without showing full package
     *     names
     */
    @Override
    @SideEffectFree
    public String formatAnnotationMirror(AnnotationMirror anno) {
        return AnnotationUtils.toStringSimple(anno);
    }
}
