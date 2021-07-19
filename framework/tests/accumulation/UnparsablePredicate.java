import org.checkerframework.framework.testchecker.testaccumulation.qual.*;

public class UnparsablePredicate {

  // :: error: predicate
  void unclosedOpen(@TestAccumulationPredicate("(foo && bar") Object x) {}

  // :: error: predicate
  void unopenedClose(@TestAccumulationPredicate("foo || bar)") Object x) {}

  // :: error: predicate
  void badKeywords1(@TestAccumulationPredicate("foo OR bar") Object x) {}

  // :: error: predicate
  void badKeywords2(@TestAccumulationPredicate("foo AND bar") Object x) {}

  // These tests check that valid Java identifiers don't cause problems
  // when evaluating predicates. Examples of identifiers from
  // https://docs.oracle.com/javase/specs/jls/se8/html/jls-3.html#jls-3.8

  void jls0Example(@TestAccumulationPredicate("String") Object x) {}

  void callJls0Example(@TestAccumulation("String") Object y) {
    jls0Example(y);
  }

  void jls1Example(@TestAccumulationPredicate("i3") Object x) {}

  void callJls1Example(@TestAccumulation("i3") Object y) {
    jls1Example(y);
  }

  void jls2Example(@TestAccumulationPredicate("αρετη") Object x) {}

  void callJls2Example(@TestAccumulation("αρετη") Object y) {
    jls2Example(y);
  }

  void jls3Example(@TestAccumulationPredicate("MAX_VALUE") Object x) {}

  void callJls3Example(@TestAccumulation("MAX_VALUE") Object y) {
    jls3Example(y);
  }

  void jls4Example(@TestAccumulationPredicate("isLetterOrDigit") Object x) {}

  void callJls4Example(@TestAccumulation("isLetterOrDigit") Object y) {
    jls4Example(y);
  }
}
