import org.checkerframework.checker.nullness.qual.*;

abstract class References {

  void context(References c) {

    // No error
    FuncA funcA1 = References::aMethod1;
    // No error, covariant parameters
    FuncA funcA2 = References::aMethod2;
    // :: error: (methodref.return)
    FuncA funcA3 = References::aMethod3;
    // :: error: (methodref.return)
    FuncA funcA4 = References::aMethod4;

    // :: error: (methodref.param)
    FuncB funcB1 = References::aMethod1;
    // No error
    FuncB funcB2 = References::aMethod2;
    // :: error: (methodref.return) :: error: (methodref.param)
    FuncB funcB3 = References::aMethod3;
    // :: error: (methodref.return)
    FuncB funcB4 = References::aMethod4;

    FuncA typeArg1 = References::<@NonNull String>aMethod5;
    // :: error: (methodref.param)
    FuncB typeArg2 = References::<@NonNull String>aMethod5;
  }

  abstract @NonNull String aMethod1(@NonNull String s);

  abstract @NonNull String aMethod2(@Nullable String s);

  abstract @Nullable String aMethod3(@NonNull String s);

  abstract @Nullable String aMethod4(@Nullable String s);

  abstract <T> T aMethod5(T t);

  interface FuncA {
    @NonNull String method(References a, @NonNull String b);
  }

  interface FuncB {
    @NonNull String method(References a, @Nullable String b);
  }
}
