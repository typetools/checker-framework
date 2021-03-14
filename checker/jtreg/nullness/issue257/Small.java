import org.checkerframework.checker.nullness.qual.Nullable;

class Gen<T extends Gen<T>> {

  static @Nullable Gen<?> newBuilder() {
    return null;
  }
}

public class Small {
  void buildGen() {
    Gen<? extends Gen<?>> builder = Gen.newBuilder();
  }
}
