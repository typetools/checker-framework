import org.checkerframework.checker.nullness.qual.NonNull;

public class ClientBuilder<T extends @NonNull ClientBuilder<T>> {

  static @NonNull ClientBuilder<?> newBuilder() {
    return new BuilderImpl();
  }

  // Dummy class to get the recursive Builder typing right.
  static class BuilderImpl extends ClientBuilder<BuilderImpl> {}

  T setThing() {
    return (T) this;
  }
}
