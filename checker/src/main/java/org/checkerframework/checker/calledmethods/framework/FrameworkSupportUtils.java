package org.checkerframework.checker.calledmethods.framework;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;

/** A utility class of static methods used in supporting builder-generation frameworks. */
public class FrameworkSupportUtils {
    /** This class is non-instantiable */
    private FrameworkSupportUtils() {}

    /**
     * Checks whether the given type is one of the immutable collections defined in
     * com.google.common.collect.
     *
     * @param type a type
     * @return whether the type is a Guava immutable collection
     */
    public static boolean isGuavaImmutableType(TypeMirror type) {
        return type.toString().startsWith("com.google.common.collect.Immutable");
    }

    /**
     * Capitalizes the first letter of the given string.
     *
     * @param prop a String
     * @return the same String, but with the first character capitalized
     */
    public static String capitalize(String prop) {
        return prop.substring(0, 1).toUpperCase() + prop.substring(1);
    }

    /**
     * Given an annotation name, return true if the element has the annotation of that name.
     *
     * @param element the element
     * @param annotName name of the annotation
     * @return true if the element has the annotation of that name
     */
    public static boolean hasAnnotation(Element element, String annotName) {
        return element.getAnnotationMirrors().stream()
                .anyMatch(anm -> AnnotationUtils.areSameByName(anm, annotName));
    }
}
