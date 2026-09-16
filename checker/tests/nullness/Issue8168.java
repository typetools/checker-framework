// Test case for https://github.com/typetools/checker-framework/issues/8168 .
// A class may extend an inner class of a class that does not enclose it.  The type arguments for
// the outer class then come from the subclass's superclass type, not from an enclosing type.

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue8168 {
  void directSubclass(Sub8168 sub, @Nullable String nble, @NonNull String nn) {
    sub.use(nn);
    // :: error: (argument)
    sub.use(nble);
    // If T were not substituted, its type would be `T extends @Nullable Object` and this
    // assignment would be an error.
    @NonNull String s = sub.get();
  }

  void indirectSubclass(SubSub8168 sub, @Nullable String nble, @NonNull String nn) {
    sub.use(nn);
    // :: error: (argument)
    sub.use(nble);
    @NonNull String s = sub.get();
  }

  // `EnclosedSub8168` is lexically enclosed by a parameterization of `Gen8168` and also extends an
  // inner class of a different parameterization of it.  The superclass type wins.
  void enclosingClassAlsoParameterizes(
      Outer8168.EnclosedSub8168 sub, @Nullable String nble, @NonNull String nn) {
    sub.use(nn);
    // :: error: (argument)
    sub.use(nble);
    @NonNull String s = sub.get();
  }
}

class Gen8168<T extends @Nullable Object> {
  class Inner {
    void use(T arg) {}

    T get() {
      throw new AssertionError();
    }
  }

  class GenericInner<U extends @Nullable Object> {
    GenericInner(U arg) {}

    void use(T t, U u) {}

    U getInner() {
      throw new AssertionError();
    }
  }
}

class Sub8168 extends Gen8168<@NonNull String>.Inner {
  Sub8168(Gen8168<@NonNull String> outer) {
    outer.super();
  }

  void callInherited(@Nullable String nble, @NonNull String nn) {
    use(nn);
    // :: error: (argument)
    use(nble);
    @NonNull String s = get();
  }
}

// The enclosing instance of the qualified super constructor invocation does not instantiate
// `GenericInner`'s own type variable `U`; the superclass type `Gen8168<...>.GenericInner<...>`
// does.
class GenericInnerSub8168 extends Gen8168<@NonNull String>.GenericInner<@NonNull Integer> {
  GenericInnerSub8168(Gen8168<@NonNull String> outer, @NonNull Integer nn) {
    outer.super(nn);
  }

  GenericInnerSub8168(Gen8168<@NonNull String> outer, @Nullable Integer nble, boolean dummy) {
    // :: error: (argument)
    outer.super(nble);
  }

  void callInherited(@Nullable String nble, @NonNull String nn, @NonNull Integer nnInt) {
    use(nn, nnInt);
    // :: error: (argument)
    use(nble, nnInt);
    @NonNull Integer i = getInner();
  }
}

class SubSub8168 extends Sub8168 {
  SubSub8168(Gen8168<@NonNull String> outer) {
    super(outer);
  }
}

class Outer8168 extends Gen8168<@Nullable String> {
  class EnclosedSub8168 extends Gen8168<@NonNull String>.Inner {
    EnclosedSub8168(Gen8168<@NonNull String> outer) {
      outer.super();
    }

    void callInherited(@Nullable String nble, @NonNull String nn) {
      use(nn);
      // :: error: (argument)
      use(nble);
      @NonNull String s = get();
    }
  }
}
