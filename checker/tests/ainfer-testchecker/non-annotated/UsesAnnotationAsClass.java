// Tests that WPI doesn't break when an annotation name is used
// as a class name. This is legal Java, and we encountered it in
// Apache Hadoop.

import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling2;

public class UsesAnnotationAsClass {
  public static String test(@AinferSibling2 AinferSibling1 annotation) {
    String value = annotation.value();
    // goal of this is to trigger inference for AinferSibling1's definition.
    String anotherValue = annotation.anotherValue();
    return value + anotherValue;
  }
}
