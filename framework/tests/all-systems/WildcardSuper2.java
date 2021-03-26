import java.util.List;

// See also checker/nullness/generics/WildcardOverride.java

interface AInterface<T> {
  public abstract int transform(List<? super T> function);
}

class B implements AInterface<Object> {
  // This shouldn't work for nullness as the function won't take possibly nullable values.
  @SuppressWarnings({"nullness", "fenum:override.param.invalid", "aliasing"})
  @Override
  public int transform(List<Object> function) {
    return 0;
  }
}
