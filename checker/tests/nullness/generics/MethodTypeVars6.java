import org.checkerframework.checker.nullness.qual.*;

class APair<S extends @Nullable Object, T extends @Nullable Object> {
  static <U extends @Nullable Object, V extends @Nullable Object> APair<U, V> of(U p1, V p2) {
    return new APair<U, V>();
  }

  static <U extends @Nullable Object, V extends @Nullable Object> APair<U, V> of2(U p1, V p2) {
    return new APair<>();
  }
}

class PairSub<SS extends @Nullable Object, TS extends @Nullable Object> extends APair<SS, TS> {
  static <US extends @Nullable Object, VS extends @Nullable Object> PairSub<US, VS> of(
      US p1, VS p2) {
    return new PairSub<US, VS>();
  }
}

class PairSubSwitching<SS extends @Nullable Object, TS extends @Nullable Object>
    extends APair<TS, SS> {
  static <US extends @Nullable Object, VS extends @Nullable Object> PairSubSwitching<US, VS> ofPSS(
      US p1, VS p2) {
    return new PairSubSwitching<US, VS>();
  }
}

class Test1<X extends @Nullable Object> {
  APair<@Nullable X, @Nullable X> test1(@Nullable X p) {
    return APair.<@Nullable X, @Nullable X>of(p, (X) null);
  }
}

class Test2<X extends @Nullable Object> {
  APair<@Nullable X, @Nullable X> test1(@Nullable X p) {
    return APair.of(p, (@Nullable X) null);
  }
  /*
  APair<@Nullable X, @Nullable X> test2(@Nullable X p) {
      // TODO cast: should this X mean the same as above??
      return APair.of(p, (X) null);
  }
  */
}

class Test3<X extends @Nullable Object> {
  APair<@NonNull X, @NonNull X> test1(@Nullable X p) {
    // :: error: (return.type.incompatible)
    return APair.of(p, (X) null);
  }
}

class Test4 {
  APair<@Nullable String, Integer> psi = PairSub.of("Hi", 42);
  APair<@Nullable String, Integer> psi2 = PairSub.of(null, 42);
  // :: error: (assignment.type.incompatible)
  APair<String, Integer> psi3 = PairSub.of(null, 42);

  APair<@Nullable String, Integer> psisw = PairSubSwitching.ofPSS(42, null);
  // :: error: (assignment.type.incompatible)
  APair<String, Integer> psisw2 = PairSubSwitching.ofPSS(42, null);
}
