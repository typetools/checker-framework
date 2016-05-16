import java.util.List;

// See also checker/nullness/generics/WildcardOverride.java

interface A<T> {
  public abstract int transform(List<? super T> function);
}

class B implements A<Object> {
  // This shouldn't work for nullness as the function won't take possibly nullable values
  @SuppressWarnings("nullness")
  @Override
  public int transform(List<Object> function) {
    return 0;
  }
}
