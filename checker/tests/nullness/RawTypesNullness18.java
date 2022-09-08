// @below-java18-jdk-skip-test

import org.checkerframework.checker.nullness.qual.Nullable;

class Generic18<G extends @Nullable Object> {}

class MyClass18 extends Generic18<MyClass18> {}

class BoundedGeneric18<B extends @Nullable CharSequence> {}

class RawTypesNullness18 {
  Generic18 rawReturn() {
    return new Generic18();
  }

  Generic18 rawField = new Generic18();
}

class TestBounded18 {
  BoundedGeneric18 rawReturn() {
    return new BoundedGeneric18<>();
  }

  BoundedGeneric18 rawField = new BoundedGeneric18();

  void useBoundedWildCard() {
    BoundedGeneric18 rawLocal = new BoundedGeneric18<String>();
    BoundedGeneric18<? extends Object> generic1 = rawReturn();
    BoundedGeneric18<? extends Object> generic2 = rawField;
    BoundedGeneric18<? extends Object> generic3 = rawLocal;
  }
}
