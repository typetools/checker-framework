import org.checkerframework.checker.nullness.qual.*;

public class OptionsTest {

    class MyAnnotation {}

    // Annotated identically to java.lang.reflect.Field.getAnnotation
    public static <T1 extends @Nullable MyAnnotation> @Nullable T1 getAnnotation(
            Class<@NonNull T1> obj) {
        return null;
    }

    public static @Nullable MyAnnotation safeGetAnnotationNonGeneric(
            Class<@NonNull MyAnnotation> annotationClass) {
        @Nullable MyAnnotation cast = getAnnotation(annotationClass);
        @Nullable MyAnnotation annotation = cast;
        return annotation;
    }

    public static <T2 extends MyAnnotation> @Nullable T2 safeGetAnnotationGeneric(
            Class<@NonNull T2> annotationClass) {
        @Nullable T2 cast = getAnnotation(annotationClass);
        @Nullable T2 annotation = cast;
        return annotation;
    }
}

/* Local Variables: */
/* compile-command: "javac -processor org.checkerframework.checker.nullness.NullnessChecker -Xbootclasspath/p:$CHECKERFRAMEWORK/checker/dist/jdk8.jar OptionsTest.java" */
/* compile-history: ("javac -processor org.checkerframework.checker.nullness.NullnessChecker -Xbootclasspath/p:$CHECKERFRAMEWORK/checker/dist/jdk8.jar OptionsTest.java") */
/* End: */
