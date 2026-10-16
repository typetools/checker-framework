// Tests that WPI doesn't break when an we attempt to infer something
// about a member of an annotation declaration (in this case, the receivers
// of value() and anotherValue() in AinferSibling1's definition).

import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling2;

public class UsesAnnotationAsClass {
  public static String test(@AinferSibling2 AinferSibling1 annotation) {
    String value = annotation.value();
    // goal of this is to trigger inference for AinferSibling1's definition.
    String anotherValue = annotation.anotherValue();
    return value + anotherValue;
  }
}
