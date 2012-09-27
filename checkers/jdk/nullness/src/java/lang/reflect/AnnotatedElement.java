package java.lang.reflect;

import java.lang.annotation.Annotation;
import checkers.nullness.quals.*;

public interface AnnotatedElement {
    boolean isAnnotationPresent(Class<? extends Annotation> arg0);
    <T extends Annotation> @Nullable T getAnnotation(Class<T> arg0);
    Annotation[] getAnnotations();
    Annotation[] getDeclaredAnnotations();
}
