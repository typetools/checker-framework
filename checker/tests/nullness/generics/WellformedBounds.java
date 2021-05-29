import org.checkerframework.checker.nullness.qual.*;

class Param<T extends @NonNull Object> {
  // Field f needs to be set, because the upper bound is @Initialized
  // :: error: (initialization.field.uninitialized)
  T f;

  void foo() {
    // Valid, because upper bound is @Initialized @NonNull
    f.toString();
  }
}

// :: error: (type.argument)
class Invalid<S extends Param<@Nullable Object>> {
  void bar(S s) {
    s.foo();
  }

  // :: error: (type.argument)
  <M extends Param<@Nullable Object>> void foobar(M p) {}
}

interface ParamI<T extends @NonNull Object> {}

class Invalid2<
    S extends
        Number &
            // :: error: (type.argument)
            ParamI<@Nullable Object>> {}

class Invalid3 {
  <
          M extends
              Number &
                  // :: error: (type.argument)
                  ParamI<@Nullable Object>>
      void foobar(M p) {}
}
