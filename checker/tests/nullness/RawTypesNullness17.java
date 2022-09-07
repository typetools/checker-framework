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

  void useBoundedWildCard() {
    BoundedGeneric17 rawLocal = new BoundedGeneric17<String>();
    // :: warning: [unchecked] unchecked conversion
    BoundedGeneric17<? extends Object> generic1 = rawReturn();
    // :: warning: [unchecked] unchecked conversion
    BoundedGeneric17<? extends Object> generic2 = rawField;
    // :: warning: [unchecked] unchecked conversion
    BoundedGeneric17<? extends Object> generic3 = rawLocal;
  }
}
