import org.checkerframework.common.aliasing.qual.Unique;

public class InferredTypesAliasingBase {
  @SuppressWarnings("return") // a new StringBuilder is not aliased
  public static @Unique StringBuilder create() {
    return new StringBuilder();
  }
}
