public class Test {

  public sealed interface IntOrBool {}

  public record WrappedInt(int a) implements IntOrBool {}

  public record WrappedBoolean(boolean b) implements IntOrBool {}

  public int test(IntOrBool i) {
    int x = 0;
    switch (i) {
      case WrappedInt(_) -> {
        x = x + 1;
      }
      case WrappedBoolean(_) -> {
        x = x + 2;
      }
    }
    return x;
  }
}
