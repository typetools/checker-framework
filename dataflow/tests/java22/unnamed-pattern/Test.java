public class Test {

  public sealed interface IntOrBool {}

  public record WrappedInt(int a) implements IntOrBool {}

  public record WrappedBoolean(boolean b) implements IntOrBool {}

  public int test(IntOrBool i) {
    int to_increment = 0;
    switch (i) {
      case WrappedInt(_) -> {
        to_increment = to_increment + 1;
      }
      case WrappedBoolean(_) -> {
        to_increment = to_increment + 2;
      }
    }
    return to_increment;
  }
}
