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
}
