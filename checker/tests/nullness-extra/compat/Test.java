import org.checkerframework.checker.nullness.qual.NonNull;
import lib.Lib;

public class Test {
  void m() {
    @NonNull Object o = Lib.maybeGetObject();
  }
}
