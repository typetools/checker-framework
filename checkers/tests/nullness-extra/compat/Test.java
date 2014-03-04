import checkers.nullness.quals.NonNull;
import lib.Lib;

public class Test {
  void m() {
    @NonNull Object o = Lib.maybeGetObject();
  }
}
