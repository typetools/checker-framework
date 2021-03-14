import org.checkerframework.checker.nullness.qual.NonNull;

public class Class2Min {
  void test(Class1Min class1) {
    // Any error must be issued, not suppressed, for this to reproduce
    @NonNull Object o = null;
    class1.methodInstance(this);
  }
}
