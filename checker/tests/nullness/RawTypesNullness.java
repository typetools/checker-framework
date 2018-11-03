import org.checkerframework.checker.nullness.qual.Nullable;

class Generic<G extends @Nullable Object> {}

class MyClass extends Generic<MyClass> {}

class BoundedGeneric<B extends @Nullable CharSequence> {}

class RawTypesNullness {
    Generic rawReturn() {
        return new Generic();
    }

    Generic rawField = new Generic();

    void use() {
        Generic rawLocal = new Generic<>();
        Generic<?> generic1 = rawReturn();
        Generic<?> generic2 = rawField;
        Generic<?> generic3 = rawLocal;
    }
}

class TestBounded {
    BoundedGeneric rawReturn() {
        return new BoundedGeneric<>();
    }

    BoundedGeneric rawField = new BoundedGeneric();

    void useWildCard() {
        BoundedGeneric rawLocal = new BoundedGeneric<String>();
        BoundedGeneric<?> generic1 = rawReturn();
        BoundedGeneric<?> generic2 = rawField;
        BoundedGeneric<?> generic3 = rawLocal;
    }

    void useBoundedWildCard() {
        BoundedGeneric rawLocal = new BoundedGeneric<String>();
        // :: warning: [unchecked] unchecked conversion
        BoundedGeneric<? extends Object> generic1 = rawReturn();
        // :: warning: [unchecked] unchecked conversion
        BoundedGeneric<? extends Object> generic2 = rawField;
        // :: warning: [unchecked] unchecked conversion
        BoundedGeneric<? extends Object> generic3 = rawLocal;
    }

    void useBoundedWildCard2() {
        BoundedGeneric rawLocal = new BoundedGeneric<String>();
        // :: warning: [unchecked] unchecked conversion
        BoundedGeneric<? extends CharSequence> generic1 = rawReturn();
        // :: warning: [unchecked] unchecked conversion
        BoundedGeneric<? extends CharSequence> generic2 = rawField;
        // :: warning: [unchecked] unchecked conversion
        BoundedGeneric<? extends CharSequence> generic3 = rawLocal;
    }

    void useTypeArg() {
        BoundedGeneric rawLocal = new BoundedGeneric<String>();
        // :: warning: [unchecked] unchecked conversion
        BoundedGeneric<String> generic1 = rawReturn();
        // :: warning: [unchecked] unchecked conversion
        BoundedGeneric<String> generic2 = rawField;
        // :: warning: [unchecked] unchecked conversion
        BoundedGeneric<String> generic3 = rawLocal;
    }

    void useAnnotatedTypeArg() {
        BoundedGeneric rawLocal = new BoundedGeneric<String>();
        // :: warning: [unchecked] unchecked conversion
        BoundedGeneric<@Nullable String> generic1 = rawReturn();
        // :: warning: [unchecked] unchecked conversion
        BoundedGeneric<@Nullable String> generic2 = rawField;
        // :: warning: [unchecked] unchecked conversion
        BoundedGeneric<@Nullable String> generic3 = rawLocal;
    }
}
