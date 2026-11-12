import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

// Unboxing a Boolean condition keeps the facts that hold when the condition is true or false.
public class UnboxingKeepsConditionalRefinement {
  @EnsuresNonNullIf(expression = "#1", result = true)
  static Boolean isNN(@Nullable Object o) {
    return o != null;
  }

  @EnsuresNonNullIf(expression = "#1", result = false)
  static Boolean isNull(@Nullable Object o) {
    return o == null;
  }

  void ifThen(@Nullable Object o) {
    if (isNN(o)) {
      o.hashCode();
    }
  }

  void ifElse(@Nullable Object o) {
    if (isNull(o)) {
      // :: error: [dereference.of.nullable]
      o.hashCode();
    } else {
      o.hashCode();
    }
  }

  void cast(@Nullable Object o) {
    if ((Boolean) (o != null)) {
      o.hashCode();
    }
  }

  // The unboxed expression has a type-variable type whose bound is Boolean.
  @SuppressWarnings("unchecked")
  <T extends Boolean> void castToTypeVariable(@Nullable Object o) {
    if ((T) isNN(o)) {
      o.hashCode();
    }
  }

  @SuppressWarnings("unchecked")
  <T extends Boolean> void castToTypeVariableElse(@Nullable Object o) {
    if ((T) isNull(o)) {
      // :: error: [dereference.of.nullable]
      o.hashCode();
    } else {
      o.hashCode();
    }
  }

  void whileLoop(@Nullable Object o) {
    while (isNN(o)) {
      o.hashCode();
    }
  }

  void whileExit(@Nullable Object o) {
    while (isNull(o)) {}
    o.hashCode();
  }

  void doWhileExit(@Nullable Object o) {
    do {} while (isNull(o));
    o.hashCode();
  }

  void forLoop(@Nullable Object o) {
    for (; isNN(o); ) {
      o.hashCode();
    }
  }

  int ternary(@Nullable Object o) {
    return isNN(o) ? o.hashCode() : 0;
  }

  void not(@Nullable Object o) {
    if (!isNull(o)) {
      o.hashCode();
    }
  }

  // In the following, a fact that holds when isNN(o) is true does not hold when the whole
  // condition is true.

  void bitwiseOr(@Nullable Object o, Boolean c) {
    if (c | isNN(o)) {
      // :: error: [dereference.of.nullable]
      o.hashCode();
    }
  }

  void bitwiseXor(@Nullable Object o, Boolean c) {
    if (c ^ isNN(o)) {
      // :: error: [dereference.of.nullable]
      o.hashCode();
    }
  }

  void equalTo(@Nullable Object o, boolean b) {
    if (b == isNN(o)) {
      // :: error: [dereference.of.nullable]
      o.hashCode();
    }
  }

  void notEqual(@Nullable Object o, Boolean b) {
    if (isNN(o) != b) {
      // :: error: [dereference.of.nullable]
      o.hashCode();
    }
  }

  void ternaryOperand(@Nullable Object o, boolean c) {
    if (c ? isNN(o) : true) {
      // :: error: [dereference.of.nullable]
      o.hashCode();
    }
  }

  void bitwiseAnd(@Nullable Object o, Boolean c) {
    if (c & isNN(o)) {
      o.hashCode();
    }
  }

  // The facts that hold when an operand is true or false do not necessarily hold when the
  // enclosing expression is true or false.

  void unrefinedLeftOfBitwiseOr(@Nullable Object o, Boolean b) {
    if (isNN(o) | b) {
      // :: error: [dereference.of.nullable]
      o.hashCode();
    }
  }

  void unrefinedLeftOfXor(@Nullable Object o, Boolean b) {
    if (isNN(o) ^ b) {
      // :: error: [dereference.of.nullable]
      o.hashCode();
    }
  }

  void unrefinedLeftOfEqualTo(@Nullable Object o, boolean x) {
    if (isNN(o) == x) {
      // :: error: [dereference.of.nullable]
      o.hashCode();
    }
  }

  // A boxed constant, which is unboxed because the other operand is primitive.

  @EnsuresNonNullIf(expression = "#1", result = true)
  static boolean isNNPrim(@Nullable Object o) {
    return o != null;
  }

  void equalToBoxedConstant(@Nullable Object o) {
    if (isNNPrim(o) == (Boolean) true) {
      o.hashCode();
    }
  }

  void notEqualBoxedConstant(@Nullable Object o) {
    if (isNNPrim(o) != (Boolean) true) {
      // :: error: [dereference.of.nullable]
      o.hashCode();
    } else {
      o.hashCode();
    }
  }

  void notBoxedConstant(@Nullable Object o) {
    if (isNNPrim(o) == !(Boolean) false) {
      o.hashCode();
    }
  }

  void orNegatedBoxedTrue(@Nullable Object o) {
    if (isNNPrim(o) | !(Boolean) true) {
    } else {
      // :: error: [dereference.of.nullable]
      o.hashCode();
    }
  }
}
