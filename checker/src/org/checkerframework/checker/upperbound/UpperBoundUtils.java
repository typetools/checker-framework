package org.checkerframework.checker.upperbound;

import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.upperbound.qual.LTEqLengthOf;
import org.checkerframework.checker.upperbound.qual.LTLengthOf;
import org.checkerframework.checker.upperbound.qual.LTOMLengthOf;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;

public class UpperBoundUtils {

    /**
     * Used to get the list of array names that an annotation applies to. Can return null if the
     * list would be empty.
     */
    public static String[] getValue(AnnotationMirror anno) {
        if (!AnnotationUtils.hasElementValue(anno, "value")) {
            return null;
        }
        return AnnotationUtils.getElementValueArray(anno, "value", String.class, true)
                .toArray(new String[0]);
    }

    /**
     * Determines if the given string is a member of the LTL or LTOM annotation attached to type.
     */
    public static boolean hasValue(AnnotatedTypeMirror type, String name) {
        String[] rgst;
        if (type.hasAnnotation(LTLengthOf.class)) {
            rgst = getValue(type.getAnnotation(LTLengthOf.class));
        } else if (type.hasAnnotation(LTOMLengthOf.class)) {
            rgst = getValue(type.getAnnotation(LTOMLengthOf.class));
        } else {
            return false;
        }
        for (String st : rgst) {
            if (st.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /** Checks if a given annotation is one of those that can have a value. */
    public static boolean hasValueMethod(AnnotationMirror anno) {
        boolean fLTL = AnnotationUtils.areSameByClass(anno, LTLengthOf.class);
        boolean fLTEL = AnnotationUtils.areSameByClass(anno, LTEqLengthOf.class);
        return fLTL || fLTEL;
    }
}
