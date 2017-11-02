import org.checkerframework.checker.nullness.qual.*;

// Nullable receivers don't make a lot of sense
// But this class tests the sub supertype recevier relationships.
// It could just use tainted.

interface Unbound1 {
    void apply(@NonNull MyClass my);
}

interface Unbound2 {
    void apply(@Nullable MyClass my);
}

interface Supplier1<R extends @Nullable Object> {
    R supply();
}

interface Bound {
    void apply();
}

class MyClass {
    void take(@NonNull MyClass this) {}

    void context1(@Nullable MyClass this, @NonNull MyClass my1, @Nullable MyClass my2) {

        Unbound1 u1 = MyClass::take;
        // :: error: (methodref.receiver.invalid)
        Unbound2 u2 = MyClass::take;

        Bound b1 = my1::take;
        // :: error: (methodref.receiver.bound.invalid)
        Bound b2 = my2::take;

        // :: error: (methodref.receiver.bound.invalid)
        Bound b11 = this::take;
    }

    void context2(@NonNull MyClass this) {
        Bound b21 = this::take;
    }

    class MySubClass extends MyClass {

        void context1(@Nullable MySubClass this) {
            // :: error: (methodref.receiver.bound.invalid)
            Bound b = super::take;
        }

        void context2(@NonNull MySubClass this) {
            Bound b = super::take;
        }

        class Nested {
            void context1(@Nullable Nested this) {
                // :: error: (methodref.receiver.bound.invalid)
                Bound b = MySubClass.super::take;
            }

            void context2(@NonNull Nested this) {
                Bound b = MySubClass.super::take;
            }
        }
    }
}

class Outer {
    class Inner1 {
        Inner1(@Nullable Outer Outer.this) {}
    }

    class Inner2 {
        Inner2(@NonNull Outer Outer.this) {}
    }

    void context(@Nullable Outer this) {
        // This one is unbound and needs an Outer as a param
        Supplier1<Inner1> f1 = Inner1::new;
        // :: error: (methodref.receiver.bound.invalid)
        Supplier1<Inner2> f2 = Inner2::new;

        // Supplier1</*3*/Inner> f = /*4*/Inner::new;
        // 4 <: 3? Constructor annotations?
    }
}
