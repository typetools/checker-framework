import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.NonNull;

public class Suppression {

  @NonNull Suppression t;

  @SuppressWarnings("initialization.fields.uninitialized")
  public Suppression(Suppression arg) {}

  @SuppressWarnings({"nullness"})
  void foo(@UnknownInitialization Suppression arg) {
    t = arg; // initialization error
    t = null; // nullness error
  }

  void test() {
    @SuppressWarnings("nullness:assignment.type.incompatible")
    @NonNull String s = null;
  }
}
