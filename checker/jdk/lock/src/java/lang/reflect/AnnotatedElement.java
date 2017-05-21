package java.lang.reflect;

import java.lang.annotation.Annotation;
import org.checkerframework.checker.lock.qual.*;

public interface AnnotatedElement {
     boolean isAnnotationPresent(@GuardSatisfied AnnotatedElement this,Class<? extends Annotation> arg0);
    <T extends Annotation> T getAnnotation(Class<T> arg0);
    Annotation[] getAnnotations();
    Annotation[] getDeclaredAnnotations();
}
