package java.lang.reflect;
import checkers.javari.quals.*;

import java.lang.annotation.Annotation;

public class AccessibleObject implements AnnotatedElement {
    public static void setAccessible(AccessibleObject[] array, boolean flag) {
        throw new RuntimeException("skeleton method");
    }

    public void setAccessible(boolean flag) throws SecurityException {
        throw new RuntimeException("skeleton method");
    }

    public boolean isAccessible(@ReadOnly AccessibleObject this) {
        throw new RuntimeException("skeleton method");
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        throw new RuntimeException("skeleton method");
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        throw new RuntimeException("skeleton method");
    }

    public Annotation[] getAnnotations() {
        throw new RuntimeException("skeleton method");
    }

    public Annotation[] getDeclaredAnnotations()  {
        throw new RuntimeException("skeleton method");
    }
}
