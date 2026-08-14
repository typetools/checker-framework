// The type argument for the function type's thrown type is a wildcard that mentions an inference
// variable.  The thrown type of the function type is therefore neither a proper type nor a use of
// an inference variable; it is an `InferenceType`.
// `InferenceFactory.getCheckedExceptionConstraints` must not assume that every thrown type that is
// not proper is a use of an inference variable.
//
// This file must not be annotated with `@SuppressWarnings("all")`, because that also suppresses
// the `type.argument.inference.crashed` error that this test checks for.

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

  static void useLambda() throws Exception {
    String s = call(() -> "");
  }

  static void useMemberReference() throws Exception {
    String s = call(CheckedExceptionWildcardThrows::id);
  }
}
