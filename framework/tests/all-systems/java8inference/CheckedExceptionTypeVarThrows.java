@SuppressWarnings("all") // Just check for crashes.
class CheckedExceptionTypeVarThrows {

  interface ThrowingSupplier<T, E extends Exception> {
    T get() throws E;
  }

  static <T, E extends Exception> T call(ThrowingSupplier<T, E> supplier) throws E {
    return supplier.get();
  }

  static <X extends Exception> String mayThrow() throws X {
    return "";
  }

  static String throwsCheckedException() throws java.io.IOException {
    return "";
  }

  // The lambda's thrown types are [X] before substitution and [] after substitution.
  static void oneThrownType() throws Exception {
    String s = call(() -> CheckedExceptionTypeVarThrows.<RuntimeException>mayThrow());
  }

  // The lambda's thrown types are [X, IOException] before substitution and [IOException] after
  // substitution.
  static void twoThrownTypes() throws Exception {
    String s =
        call(
            () -> {
              CheckedExceptionTypeVarThrows.<RuntimeException>mayThrow();
              return throwsCheckedException();
            });
  }
}
