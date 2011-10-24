import java.lang.reflect.*;
import java.lang.annotation.Annotation;
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
            // TODO: Why is the @NonRaw needed?
            annotation = f.getAnnotation( (Class<@NonNull @NonRaw T>) annotationClass);
        } catch (Exception e) {
            annotation = null;
        }
        return annotation;
    }

    private <T extends Annotation> @Nullable T
    safeGetAnnotation2(Field f, Class<@NonNull @NonRaw T> annotationClass) {
        @Nullable T annotation;
        try {
            annotation = f.getAnnotation(annotationClass);
        } catch (Exception e) {
            annotation = null;
        }
        return annotation;
    }
}
