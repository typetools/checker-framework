package org.checkerframework.common.returnsrcvr.framework;

import java.lang.annotation.Annotation;
import java.util.EnumSet;
import javax.lang.model.element.Element;
import org.checkerframework.common.returnsrcvr.ReturnsRcvrChecker;
import org.checkerframework.javacutil.AnnotationUtils;

/** A utility class for framework support in returns receiver checker */
public class FrameworkSupportUtils {

    // this class is non-instantiable
    private FrameworkSupportUtils() {}

    public enum Framework {
        AUTO_VALUE,
        LOMBOK;
    }

    /**
     * Determine the framework supports that should be disabled according to the flag {@code
     * disableFrameworkSupports}, return a EnumSet containing the framework supports in use
     *
     * @param option a comma-separated list of frameworks whose support should be disabled
     * @return an EnumSet of all framework supports in use
     */
    public static EnumSet<Framework> getFrameworkSet(String option) {
        EnumSet<Framework> frameworkSet = EnumSet.allOf(Framework.class);

        if (option != null) {
            for (String disabledFrameworkSupport : option.split("\\s?,\\s?")) {
                switch (disabledFrameworkSupport.toUpperCase()) {
                    case ReturnsRcvrChecker.AUTOVALUE_SUPPORT:
                        frameworkSet.remove(Framework.AUTO_VALUE);
                        break;
                    case ReturnsRcvrChecker.LOMBOK_SUPPORT:
                        frameworkSet.remove(Framework.LOMBOK);
                        break;
                }
            }
        }
        return frameworkSet;
    }

    /**
     * Given an annotation class, return true if the element has the annotation
     *
     * @param element the element that might have an annotation
     * @param annotClass the class of the annotation that might be present
     * @return true if the element has the annotation
     */
    public static boolean hasAnnotation(Element element, Class<? extends Annotation> annotClass) {
        return element.getAnnotationMirrors().stream()
                .anyMatch(anm -> AnnotationUtils.areSameByClass(anm, annotClass));
    }

    /**
     * Given an annotation name, return true if the element has the annotation of that name
     *
     * @param element the element that might have an annotation
     * @param annotClassName the class of the annotation that might be present
     * @return true if the element has the annotation of that class
     */
    public static boolean hasAnnotationByName(Element element, String annotClassName) {
        return element.getAnnotationMirrors().stream()
                .anyMatch(anm -> AnnotationUtils.areSameByName(anm, annotClassName));
    }
}
