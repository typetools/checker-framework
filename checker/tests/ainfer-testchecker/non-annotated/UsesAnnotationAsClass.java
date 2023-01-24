// Tests that WPI doesn't break when an annotation name is used
// as a class name. This is legal Java, and we encountered it in
// Apache Hadoop.

import org.checkerframework.common.value.qual.StringVal;

public class UsesAnnotationAsClass {
  public static String test(StringVal annotation) {
    String[] value = annotation.value();
    if (value.length == 1) {
      return value[0];
    } else {
      return "an array";
    }
  }

  public static void useTest(StringVal annotation) {
    test(annotation);
  }
}
