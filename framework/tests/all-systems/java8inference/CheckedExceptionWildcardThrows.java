// The type argument for the function type's thrown type is a wildcard that mentions an inference
// variable.  `InferenceFactory.getCheckedExceptionConstraints` must derive the function type from
// the non-wildcard parameterization (JLS 9.9) of the target type.  Otherwise the thrown type is
// neither a proper type nor a use of an inference variable, so no checked exception constraint is
// created for it.

@SuppressWarnings("all") // Just check for crashes.
class CheckedExceptionWildcardThrows {

  interface ThrowingSupplier<T, E extends Exception> {
    T get() throws E;
  }

  static <T, E extends Exception> T call(ThrowingSupplier<T, ? extends E> supplier) throws E {
    return supplier.get();
  }

  static String id() {
    return "";
  }

  static String throwsCheckedException() throws java.io.IOException {
    return "";
  }

  static void useLambda() throws Exception {
    String s = call(() -> "");
  }

  static void useMemberReference() throws Exception {
    String s = call(CheckedExceptionWildcardThrows::id);
  }

  // The lambda body throws a checked exception, so the inference variable for the function type's
  // thrown type gets a checked exception constraint and `E` is inferred as IOException.  (If `E`
  // were inferred as its upper bound, Exception, then `call` would throw Exception, which this
  // method does not declare.)
  static void useThrowingLambda() throws java.io.IOException {
    String s = call(() -> throwsCheckedException());
  }

  static void useThrowingMemberReference() throws java.io.IOException {
    String s = call(CheckedExceptionWildcardThrows::throwsCheckedException);
  }
}
