import org.checkerframework.checker.nullness.qual.*;

class OGSuper<S extends @Nullable Object> {
  public void m(S p) {}
}

class OGImpl1<T extends @NonNull Object> extends OGSuper<T> {
  public void m(T p) {}
}

class OGImpl2<T> extends OGSuper<T> {
  public void m(T p) {}
}
