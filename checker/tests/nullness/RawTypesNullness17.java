// @above-java17-jdk-skip-test

import org.checkerframework.checker.nullness.qual.Nullable;

class Generic17<G extends @Nullable Object> {}

class MyClass17 extends Generic17<MyClass17> {}

class BoundedGeneric17<B extends @Nullable CharSequence> {}

class RawTypesNullness17 {
  Generic17 rawReturn() {
    return new Generic17();
  }

  Generic17 rawField = new Generic17();
}

class TestBounded17 {
  BoundedGeneric17 rawReturn() {
    return new BoundedGeneric17<>();
  }

  BoundedGeneric17 rawField = new BoundedGeneric17();

  @SuppressWarnings("unchecked") // only needed on JDK 17 and lower
  void useBoundedWildCard() {
    BoundedGeneric17 rawLocal = new BoundedGeneric17<String>();
    BoundedGeneric17<? extends Object> generic1 = rawReturn();
    BoundedGeneric17<? extends Object> generic2 = rawField;
    BoundedGeneric17<? extends Object> generic3 = rawLocal;
  }
}
