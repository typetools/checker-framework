import org.checkerframework.checker.interning.qual.Interned;

public @Interned class RecursiveClass<
    T extends RecursiveClass<T, F>, F extends RecursiveClass<T, F>> {
  static @Interned class InternedClass {}

  static class Generic<T extends InternedClass, S extends T> {}

  static @Interned class RecursiveClass2<G extends RecursiveClass2<G>> {}
}
