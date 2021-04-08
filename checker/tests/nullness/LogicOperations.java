import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.nullness.qual.NonNull;

public class LogicOperations {
  void andTrueClause(@Nullable Object a) {
    if (a != null && helper()) {
      a.toString();
    }
  }

  void andTrueClauseReverse(@Nullable Object a) {
    if (helper() && a != null) {
      a.toString();
    }
  }

  void oneAndComplement(@Nullable Object a) {
    if (a != null && helper()) {
      a.toString();
      return;
    }
    // :: error: (dereference.of.nullable)
    a.toString(); // error
  }

  void repAndComplement(@Nullable Object a, @Nullable Object b) {
    if (a == null && b == null) {
      // :: error: (dereference.of.nullable)
      a.toString(); // error
      return;
    }
    // :: error: (dereference.of.nullable)
    a.toString(); // error
  }

  void oneOrComplement(@Nullable Object a) {
    if (a == null || helper()) {
      // :: error: (dereference.of.nullable)
      a.toString(); // error
      return;
    }
    a.toString();
  }

  void simpleOr1(@Nullable Object a, @Nullable Object b) {
    if (a != null || b != null) {
      // :: error: (dereference.of.nullable)
      a.toString(); // error
    }
  }

  void simpleOr2(@Nullable Object a, @Nullable Object b) {
    if (a != null || b != null) {
      // :: error: (dereference.of.nullable)
      b.toString(); // error
    }
  }

  void sideeffect() {
    Object a = "m";
    if ((a = null) != "n") {
      // :: error: (assignment.type.incompatible)
      @NonNull Object l1 = a;
    }
    // :: error: (assignment.type.incompatible)
    @NonNull Object l2 = a;
  }

  static boolean helper() {
    return true;
  }
}
