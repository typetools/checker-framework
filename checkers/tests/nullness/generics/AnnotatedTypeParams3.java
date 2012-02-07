import java.lang.reflect.*;
import java.lang.annotation.Annotation;
import java.util.List;
import checkers.nullness.quals.*;

class AnnotatedTypeParams3 {
    private <T extends Annotation> @Nullable T
            safeGetAnnotation(Field f, Class<T> annotationClass) {
        @Nullable T annotation;
        try {
            // Note how the assignment context influences the inferred type argument.
            // If the type parameter for getAnnotation has @NonNull as upper bound
            // this assignment wouldn't work, as @Nullable would be inferred from the
            // assignment.
            // We still need the cast, because the declared parameter type specifies
            // an upper AND lower bound of @NonNull, and the parameter annotationClass
            // has a different default lower bound.
            // As alternative, see safeGetAnnotation2 below, which instead changes
            // the parameter type.
            annotation = f.getAnnotation( (Class<@NonNull T>) annotationClass);
        } catch (Exception e) {
            annotation = null;
        }
        return annotation;
    }

    private <T extends Annotation> @Nullable T
    safeGetAnnotation2(Field f, Class<@NonNull T> annotationClass) {
        @Nullable T annotation;
        try {
            annotation = f.getAnnotation(annotationClass);
        } catch (Exception e) {
            annotation = null;
        }
        return annotation;
    }

    <@NonNull T> void test(T p) {
        Object o = p;
        @NonNull Object re = o;
    }

    <T extends @NonNull Object> void test2(T p) {
        Object o = p;
        @NonNull Object re = o;
    }

    // TODO: do we want to infer the type variable annotation on local variable "o"?
    <T> void test3(@NonNull T p) {
        T o = p;
        @NonNull T re = o;
    }

    void test5a(List<?> p) {
        p.get(0).toString();
        Object o = p.get(0);
        o.toString();
    }

    void test5b(List<? extends Object> p) {
        p.get(0).toString();
        Object o = p.get(0);
        o.toString();
    }

    void test5c(List<? extends @NonNull Object> p) {
        p.get(0).toString();
        Object o = p.get(0);
        o.toString();
    }

    void test5d(List<? extends @Nullable Object> p) {
        //:: error: (dereference.of.nullable)
        p.get(0).toString();
        Object o = p.get(0);
        //:: error: (dereference.of.nullable)
        o.toString();
    }
}
