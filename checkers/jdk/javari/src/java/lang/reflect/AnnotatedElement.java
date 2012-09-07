package java.lang.reflect;
import checkers.javari.quals.*;

import java.lang.annotation.Annotation;

public @ReadOnly interface AnnotatedElement {

    boolean isAnnotationPresent(Class<? extends Annotation> annotationClass);
    <T extends Annotation> T getAnnotation(Class<T> annotationClass);
    Annotation[] getAnnotations();
    Annotation[] getDeclaredAnnotations();
}
