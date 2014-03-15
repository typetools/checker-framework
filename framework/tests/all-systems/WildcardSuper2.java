import java.util.List;

interface A<T> {
  public abstract int transform(List<? super T> function);
}

class B implements A<Object> {
  // TODO: doesn't work for Javari and OIGJ right now
  @SuppressWarnings({"javari", "oigj"})
  @Override
  public int transform(List<Object> function) {
    return 0;
  }
}
