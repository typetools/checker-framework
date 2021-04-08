import org.checkerframework.checker.interning.qual.Interned;

public class InternedClassDecl {
  static @Interned class InternedClass {}

  static class Generic<T extends InternedClass, S extends T> {}

  static @Interned class RecursiveClass2<G extends RecursiveClass2<G>> {}
}
