import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

class RedundantAnnotationOptions {
  private static <T extends Annotation> @Nullable T safeGetAnnotation(
      Field f, Class<T> annotationClass) {
    @Nullable T annotation;
    try {
      @Nullable T cast = f.getAnnotation((Class<@NonNull T>) annotationClass);
      annotation = cast;
    } catch (Exception e) {
      annotation = null;
    }

    return annotation;
  }
}
