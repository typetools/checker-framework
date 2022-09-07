// @above-java17-jdk-skip-test

import org.checkerframework.checker.nullness.qual.Nullable;

class Generic<G extends @Nullable Object> {}

class MyClass extends Generic<MyClass> {}

class BoundedGeneric<B extends @Nullable CharSequence> {}

class RawTypesNullness {
  Generic rawReturn() {
    return new Generic();
  }

  Generic rawField = new Generic();
}

class TestBounded {
  BoundedGeneric rawReturn() {
    return new BoundedGeneric<>();
  }

  BoundedGeneric rawField = new BoundedGeneric();

  void useBoundedWildCard() {
    BoundedGeneric rawLocal = new BoundedGeneric<String>();
    // :: warning: [unchecked] unchecked conversion
    BoundedGeneric<? extends Object> generic1 = rawReturn();
    // :: warning: [unchecked] unchecked conversion
    BoundedGeneric<? extends Object> generic2 = rawField;
    // :: warning: [unchecked] unchecked conversion
    BoundedGeneric<? extends Object> generic3 = rawLocal;
  }
}
