package org.checkerframework.checker.upperbound;

import java.util.HashSet;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.samelen.SameLenAnnotatedTypeFactory;
import org.checkerframework.checker.samelen.qual.SameLen;
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
     * Determines if the given string is a member of the LTL or LTOM annotation attached to ubType.
     * Requires a SameLen annotation as well, so that it can compare the set of SameLen annotations
     * attached to the array/list to the passed string.
     */
    public static boolean hasValue(
            AnnotatedTypeMirror ubType, String name, AnnotatedTypeMirror slType) {
        String[] rgst;
        if (ubType.hasAnnotation(LTLengthOf.class)) {
            rgst = getValue(ubType.getAnnotation(LTLengthOf.class));
        } else if (ubType.hasAnnotation(LTOMLengthOf.class)) {
            rgst = getValue(ubType.getAnnotation(LTOMLengthOf.class));
        } else {
            return false;
        }

        HashSet<String> names = new HashSet<>();
        names.add(name);

        // Produce the full list of relevant names by checking the SameLen type.
        if (slType.hasAnnotation(SameLen.class)) {
            AnnotationMirror anno =
                    slType.getAnnotationInHierarchy(SameLenAnnotatedTypeFactory.UNKNOWN);
            if (AnnotationUtils.hasElementValue(anno, "value")) {
                String[] slNames =
                        AnnotationUtils.getElementValueArray(anno, "value", String.class, true)
                                .toArray(new String[0]);
                for (String st : slNames) {
                    names.add(st);
                }
            }
        }

        for (String st : rgst) {
            if (names.contains(st)) {
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
