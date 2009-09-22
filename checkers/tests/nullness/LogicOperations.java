import checkers.nullness.quals.*;

class LogicOperations {
  void andTrueClause(@Nullable Object a) {
    if (a != null && helper())
      a.toString();
  }

  void andTrueClauseReverse(@Nullable Object a) {
    if (helper() && a != null)
      a.toString();
  }

  void oneAndComplement(@Nullable Object a) {
    if (a != null && helper()) {
      a.toString();
      return;
    }
    a.toString();   // error
  }

  void repAndComplement(@Nullable Object a, @Nullable Object b) {
    if (a == null && b == null) {
      a.toString(); // error
      return;
    }
    a.toString();   // error
  }

  void oneOrComplement(@Nullable Object a) {
    if (a == null || helper()) {
      a.toString();  // error
      return;
    }
    a.toString();
  }

  void simpleOr1(@Nullable Object a, @Nullable Object b) {
      if (a != null || b != null) {
          a.toString(); // error
      }
  }

  void simpleOr2(@Nullable Object a, @Nullable Object b) {
      if (a != null || b != null) {
          b.toString(); // error
      }
  }

  void sideeffect() {
      Object a = "m";
      if ((a = null) != "n")
          a.toString();
      a.toString();
  }

  static boolean helper() { return true; }
}
