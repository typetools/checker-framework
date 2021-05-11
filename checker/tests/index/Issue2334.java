// Test case for issue #2334: http://tinyurl.com/cfissue/2334

import org.checkerframework.checker.index.qual.NonNegative;

public class Issue2334 {

  void hasSideEffect() {}

  String stringField;

  void m1(String stringFormal) {
    if (stringFormal.indexOf('d') != -1) {
      hasSideEffect();
      @NonNegative int i = stringFormal.indexOf('d');
    }
  }

  void m2() {
    if (stringField.indexOf('d') != -1) {
      hasSideEffect();
      // :: error: (assignment)
      @NonNegative int i = stringField.indexOf('d');
    }
  }

  void m3(String stringFormal) {
    if (stringFormal.indexOf('d') != -1) {
      System.out.println("hey");
      @NonNegative int i = stringFormal.indexOf('d');
    }
  }

  void m4() {
    if (stringField.indexOf('d') != -1) {
      System.out.println("hey");
      // :: error: (assignment)
      @NonNegative int i = stringField.indexOf('d');
    }
  }
}
