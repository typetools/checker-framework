import java.util.List;
import org.checkerframework.framework.testchecker.util.*;

public class Issue1967 {
  static class Box<T> {}

  void testPrimitiveArrayQualifiers(Box<@Odd int[]> boxOdd, Box<@Even int[]> boxEven) {
    // :: error: [assignment]
    boxOdd = boxEven;
  }

  void testDeclaredTypeQualifiers(Box<@Odd String> boxOdd, Box<@Even String> boxEven) {
    // :: error: [assignment]
    boxOdd = boxEven;
  }

  void testNestedTypeQualifiers(Box<List<@Odd String>> boxOdd, Box<List<@Even String>> boxEven) {
    // :: error: [assignment]
    boxOdd = boxEven;
  }

  <T> void testTypeVariableQualifiers(Box<@Odd T> boxOdd, Box<@Even T> boxEven) {
    // :: error: [assignment]
    boxOdd = boxEven;
  }

  void testMatchingTypes(Box<@Odd String> box1, Box<@Odd String> box2) {
    box1 = box2;
  }
}
