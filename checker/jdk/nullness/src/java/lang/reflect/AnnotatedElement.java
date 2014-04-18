package java.lang.reflect;

import java.lang.annotation.Annotation;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface AnnotatedElement {
    @Pure boolean isAnnotationPresent(Class<? extends Annotation> arg0);
    <T extends @Nullable Annotation> @Nullable T getAnnotation(Class<T> arg0);
    Annotation[] getAnnotations();
    Annotation[] getDeclaredAnnotations();
}
