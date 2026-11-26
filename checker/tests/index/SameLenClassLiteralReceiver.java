import org.checkerframework.checker.index.qual.SameLen;
import org.checkerframework.dataflow.qual.Pure;

// A call of an instance method on a class literal must not be confused with a call of a static
// method that has the same name:  SameLenClassLiteralReceiver.class.getName() and
// SameLenClassLiteralReceiver.getName() can differ in length.

public class SameLenClassLiteralReceiver {

  @Pure
  static String getName() {
    return "";
  }

  void instanceAndStaticMethod() {
    String s = SameLenClassLiteralReceiver.class.getName();
    String u = SameLenClassLiteralReceiver.getName();
    for (int k = 0; k < s.length(); k++) {
      // :: error: (argument)
      u.charAt(k);
    }
  }

  void sameMethod() {
    String s = SameLenClassLiteralReceiver.class.getName();
    String u = SameLenClassLiteralReceiver.class.getName();
    for (int k = 0; k < s.length(); k++) {
      u.charAt(k);
    }
  }

  // The tests below write a call on a class literal in an annotation, so the expression parser must
  // parse it.

  void annotationNamesInstanceMethod(
      @SameLen("SameLenClassLiteralReceiver.class.getName()") String u) {
    String s = SameLenClassLiteralReceiver.class.getName();
    for (int k = 0; k < s.length(); k++) {
      u.charAt(k);
    }
  }

  void annotationNamesStaticMethod(@SameLen("SameLenClassLiteralReceiver.getName()") String u) {
    String s = SameLenClassLiteralReceiver.class.getName();
    for (int k = 0; k < s.length(); k++) {
      // :: error: (argument)
      u.charAt(k);
    }
  }

  void parenthesizedClassLiteral(
      @SameLen("(SameLenClassLiteralReceiver.class).getName()") String u) {
    String s = SameLenClassLiteralReceiver.class.getName();
    for (int k = 0; k < s.length(); k++) {
      u.charAt(k);
    }
  }

  void primitiveClassLiteral(@SameLen("int.class.getName()") String u) {
    String s = int.class.getName();
    for (int k = 0; k < s.length(); k++) {
      u.charAt(k);
    }
  }

  void voidClassLiteral(@SameLen("void.class.getName()") String u) {
    String s = void.class.getName();
    for (int k = 0; k < s.length(); k++) {
      u.charAt(k);
    }
  }

  void arrayClassLiteral(@SameLen("String[].class.getName()") String u) {
    String s = String[].class.getName();
    for (int k = 0; k < s.length(); k++) {
      u.charAt(k);
    }
  }
}
