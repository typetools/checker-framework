package org.checkerframework.common.returnsreceiver.framework;

import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.EnumSet;
import javax.lang.model.element.Element;
import org.checkerframework.common.returnsreceiver.ReturnsReceiverChecker;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.UserError;

/** A utility class for framework support in the Returns Receiver Checker. */
public class FrameworkSupportUtils {

    /** this class is non-instantiable */
    private FrameworkSupportUtils() {
        throw new RuntimeException();
    }

    /**
     * Return which frameworks should be supported, respecting the command-line argument {@code
     * --disableFrameworkSupport}.
     *
     * @param disabledFrameworks a comma-separated list of frameworks whose support should be
     *     disabled; may be null
     * @return the frameworks supported by this instantiation of the Returns Receiver Checker
     */
    public static Collection<FrameworkSupport> getSupportedFrameworks(String disabledFrameworks) {
        Collection<FrameworkSupport> frameworkSupports =
                new ArrayDeque<>(EnumSet.allOf(Framework.class));

        if (disabledFrameworks != null) {
            for (String disabledFramework : disabledFrameworks.split("\\s?,\\s?")) {
                switch (disabledFramework.toUpperCase()) {
                    case ReturnsReceiverChecker.AUTOVALUE_SUPPORT:
                        frameworkSupports.remove(Framework.AUTO_VALUE);
                        break;
                    case ReturnsReceiverChecker.LOMBOK_SUPPORT:
                        frameworkSupports.remove(Framework.LOMBOK);
                        break;
                    default:
                        throw new UserError(
                                "Unrecognized framework in --disabledFrameworkSupport: "
                                        + disabledFrameworks);
                }
            }
        }
        return frameworkSupports;
    }

    /**
     * Given an annotation class, return true if the element has an annotation of that class.
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
     * Given an annotation name, return true if the element has an annotation of that name.
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
