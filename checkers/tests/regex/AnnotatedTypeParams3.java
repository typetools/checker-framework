import java.lang.reflect.*;
import java.lang.annotation.Annotation;
import checkers.regex.quals.Regex;

class AnnotatedTypeParams3 {
    private <T extends Annotation> T
            safeGetAnnotation(Field f, Class<T> annotationClass) {
        T annotation;
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
            // TODO: Why is the @NonRaw needed?
            annotation = f.getAnnotation( (Class<T>) annotationClass);
        } catch (Exception e) {
            annotation = null;
        }
        return annotation;
    }

    private <T extends Annotation> T
    safeGetAnnotation2(Field f, Class<T> annotationClass) {
        T annotation;
        try {
            annotation = f.getAnnotation(annotationClass);
        } catch (Exception e) {
            annotation = null;
        }
        return annotation;
    }

    <@Regex T> void test(T p) {
        Object o = p;
        @Regex Object re = o;
    }

    <T extends @Regex Object> void test2(T p) {
        Object o = p;
        @Regex Object re = o;
    }

    // TODO: do we want to infer the type variable annotation on local variable "o"?
    <T> void test3(@Regex T p) {
        T o = p;
        @Regex T re = o;
    }

}
