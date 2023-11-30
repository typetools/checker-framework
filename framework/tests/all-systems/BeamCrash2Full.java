import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class BeamCrash2Full {

  private static void validateGettersHaveConsistentAnnotation(
      List<PropertyDescriptor> descriptors,
      final AnnotationPredicates annotationPredicates,
      SortedSet<Method> gettersWithTheAnnotation) {
    for (final PropertyDescriptor descriptor : descriptors) {
      throw new IllegalArgumentException(
          String.format(
              "Property [%s] is marked with contradictory annotations. Found [%s].",
              descriptor.getName(),
              gettersWithTheAnnotation.stream()
                  .flatMap(
                      method ->
                          Arrays.stream(method.getAnnotations())
                              .filter(annotationPredicates.forAnnotation)
                              .map(
                                  annotation ->
                                      String.format(
                                          "[%s on %s]",
                                          formatAnnotation(annotation),
                                          formatMethodWithClass(method))))
                  .collect(Collectors.joining(", "))));
    }
  }

  public static String formatAnnotation(Annotation annotation) {
    return "";
  }

  public static String formatMethodWithClass(Method input) {
    return "";
  }

  static class AnnotationPredicates {

    Predicate<Annotation> forAnnotation;
  }
}
