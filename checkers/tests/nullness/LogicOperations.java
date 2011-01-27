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
    //:: (dereference.of.nullable)
    a.toString();   // error
  }

  void repAndComplement(@Nullable Object a, @Nullable Object b) {
    if (a == null && b == null) {
      //:: (dereference.of.nullable)
      a.toString(); // error
      return;
    }
    //:: (dereference.of.nullable)
    a.toString();   // error
  }

  void oneOrComplement(@Nullable Object a) {
    if (a == null || helper()) {
      //:: (dereference.of.nullable)
      a.toString();  // error
      return;
    }
    a.toString();
  }

  void simpleOr1(@Nullable Object a, @Nullable Object b) {
      if (a != null || b != null) {
        //:: (dereference.of.nullable)
          a.toString(); // error
      }
  }

  void simpleOr2(@Nullable Object a, @Nullable Object b) {
      if (a != null || b != null) {
        //:: (dereference.of.nullable)
          b.toString(); // error
      }
  }

  void sideeffect() {
      Object a = "m";
      if ((a = null) != "n")
        //:: (dereference.of.nullable)
          a.toString();
      //:: (dereference.of.nullable)
      a.toString();
  }

  static boolean helper() { return true; }
}
