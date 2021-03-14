import org.checkerframework.checker.calledmethods.qual.*;

public class UnparsablePredicate {

  // :: error: predicate.invalid
  void unclosedOpen(@CalledMethodsPredicate("(foo && bar") Object x) {}

  // :: error: predicate.invalid
  void unopenedClose(@CalledMethodsPredicate("foo || bar)") Object x) {}

  // :: error: predicate.invalid
  void badKeywords1(@CalledMethodsPredicate("foo OR bar") Object x) {}

  // :: error: predicate.invalid
  void badKeywords2(@CalledMethodsPredicate("foo AND bar") Object x) {}

  // These tests check that valid java identifiers don't cause problems
  // when evaluating predicates. Examples of identifiers from
  // https://docs.oracle.com/javase/specs/jls/se8/html/jls-3.html#jls-3.8

  void jls0Example(@CalledMethodsPredicate("String") Object x) {}

  void callJls0Example(@CalledMethods("String") Object y) {
    jls0Example(y);
  }

  void jls1Example(@CalledMethodsPredicate("i3") Object x) {}

  void callJls1Example(@CalledMethods("i3") Object y) {
    jls1Example(y);
  }

  // TODO: support Unicode. SPEL, which we use to parse expressions, doesn't.
  /*   void jls2Example(@CalledMethodsPredicate("αρετη") Object x) { }
  void callJls2Example(@CalledMethods("αρετη") Object y) {
      jls2Example(y);
  }*/

  void jls3Example(@CalledMethodsPredicate("MAX_VALUE") Object x) {}

  void callJls3Example(@CalledMethods("MAX_VALUE") Object y) {
    jls3Example(y);
  }

  void jls4Example(@CalledMethodsPredicate("isLetterOrDigit") Object x) {}

  void callJls4Example(@CalledMethods("isLetterOrDigit") Object y) {
    jls4Example(y);
  }
}
