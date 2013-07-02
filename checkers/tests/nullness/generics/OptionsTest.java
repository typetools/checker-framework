import java.util.*;
import checkers.nullness.quals.*;


public class OptionsTest {
  
  class MyAnnotation { }

  // Annotated identically to java.lang.reflect.Field.getAnnotation
  public static <T extends @Nullable MyAnnotation>
  @Nullable T getAnnotation(Class<@NonNull T> obj) {
    return null;
  }


  public static
  @Nullable MyAnnotation safeGetAnnotationNonGeneric(Class<@NonNull MyAnnotation> annotationClass) {
    @Nullable MyAnnotation cast = getAnnotation(annotationClass);
    @Nullable MyAnnotation annotation = cast;
    return annotation;
  }


  public static <T extends MyAnnotation>
  @Nullable T safeGetAnnotationGeneric(Class<@NonNull T> annotationClass) {
    @Nullable T cast = getAnnotation(annotationClass);
    @Nullable T annotation = cast;
    return annotation;
  }


}

/* Local Variables: */
/* compile-command: "javac -processor checkers.nullness.NullnessChecker -Xbootclasspath/p:$CHECKERS/jdk/jdk.jar OptionsTest.java" */
/* compile-history: ("javac -processor checkers.nullness.NullnessChecker -Xbootclasspath/p:$CHECKERS/jdk/jdk.jar OptionsTest.java") */
/* End: */


