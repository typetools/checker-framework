package java.lang.reflect;

import java.lang.annotation.Annotation;
import checkers.nonnull.quals.Nullable;

public interface AnnotatedElement {
    boolean isAnnotationPresent(Class<? extends Annotation> arg0);
    <T extends @Nullable Annotation> @Nullable T getAnnotation(Class<T> arg0);
    Annotation[] getAnnotations();
    Annotation[] getDeclaredAnnotations();
}
