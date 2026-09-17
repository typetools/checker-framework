// The function type of a raw functional interface type is the erasure of the function type of the
// generic functional interface (JLS 9.9).

@SuppressWarnings("rawtypes")
public class RawFunctionalInterface {
  interface Fn<InputT, OutputT> {
    OutputT apply(InputT input);
  }

  interface BoundedFn<InputT extends Number, OutputT> {
    OutputT apply(InputT input);
  }

  // `input` has the erasure of `InputT`, which is @Nullable Object.
  static Fn erasedParameter() {
    // :: error: (dereference.of.nullable)
    return input -> input.toString();
  }

  static Fn checkedParameter() {
    return input -> input == null ? "null" : input.toString();
  }

  // `input` has the erasure of `InputT extends Number`, which is @NonNull Number.
  static BoundedFn erasedBound() {
    return input -> input.intValue();
  }

  static String str(Number n) {
    return n.toString();
  }

  static BoundedFn memberReference() {
    return RawFunctionalInterface::str;
  }

  // The lambda body is checked against the erasure of `OutputT`, which is @Nullable Object, so
  // returning null is legal.
  static Fn erasedReturnType() {
    return input -> null;
  }
}
