package org.checkerframework.common.returnsreceiver.framework;

import java.lang.annotation.Annotation;
import java.util.EnumSet;
import javax.lang.model.element.Element;
import org.checkerframework.javacutil.AnnotationUtils;

/** A utility class for framework support in the Returns Receiver Checker. */
public class FrameworkSupportUtils {

    /** this class is non-instantiable */
    private FrameworkSupportUtils() {
        throw new RuntimeException();
    }

    /** @return the frameworks supported by this instantiation of the Returns Receiver Checker */
    public static EnumSet<Framework> getSupportedFrameworks() {
        return EnumSet.allOf(Framework.class);
    }

    /**
     * Given an annotation class, return true if the element has an annotation of that class.
     *
     * @param element the element that might have an annotation
     * @param annotClass the class of the annotation that might be present
     * @return true if the element has the annotation
     */
    public static boolean hasAnnotation(Element element, Class<? extends Annotation> annotClass) {
        return AnnotationUtils.containsSameByClass(element.getAnnotationMirrors(), annotClass);
    }

    /**
     * Given an annotation name, return true if the element has an annotation of that name.
     *
     * @param element the element that might have an annotation
     * @param annotClassName the class of the annotation that might be present
     * @return true if the element has the annotation of that class
     */
    public static boolean hasAnnotationByName(Element element, String annotClassName) {
        return AnnotationUtils.containsSameByName(element.getAnnotationMirrors(), annotClassName);
    }
}
