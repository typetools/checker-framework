import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class FlowFbc {

  @NonNull String f;
  @NotOnlyInitialized @NonNull String g;

  public FlowFbc(String arg) {
    // :: error: (dereference.of.nullable)
    f.toLowerCase();
    // :: error: (dereference.of.nullable)
    g.toLowerCase();
    f = arg;
    g = arg;
    foo();
    f.toLowerCase();
    // :: error: (method.invocation.invalid)
    g.toLowerCase();
    f = arg;
  }

  void test() {
    @Nullable String s = null;
    s = "a";
    s.toLowerCase();
  }

  void test2(@Nullable String s) {
    if (s != null) {
      s.toLowerCase();
    }
  }

  void foo(@UnknownInitialization FlowFbc this) {}

  // TODO Pure, etc.
}
