// Test case for the fix to "getCheckedExceptionConstraints may misalign or exhaust the
// thrown-types iterator".
//
// When inference reduces the constraint formula <LambdaExpression ->throws T> (JLS 18.2.5),
// `InferenceFactory.getCheckedExceptionConstraints` pairs up the thrown types computed by
// `CheckedExceptionsUtil.thrownCheckedExceptions` (a list of TypeMirror) with those computed by
// `CheckedExceptionsUtil.thrownCheckedExceptionsATM` (a list of AnnotatedTypeMirror).  Those are
// two independent traversals of the lambda body, and they can classify the same invocation
// differently:  the former tests the *declared* thrown type (here, the type variable `X`, which
// is a checked exception because it is a subtype of neither `RuntimeException` nor `Error`),
// whereas the latter tests the thrown type *after substitution* (here, `RuntimeException`, which
// is unchecked).  So the first list is longer than the second one, and pairing them up element by
// element threw `NoSuchElementException`.

@SuppressWarnings({"unchecked", "all"}) // Just check for crashes.
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
  // substitution.  Before the fix, the first loop iteration also paired the wrong
  // AnnotatedTypeMirror (that of `IOException`) with the TypeMirror `X`.
  static void twoThrownTypes() throws Exception {
    String s =
        call(
            () -> {
              CheckedExceptionTypeVarThrows.<RuntimeException>mayThrow();
              return throwsCheckedException();
            });
  }
}
