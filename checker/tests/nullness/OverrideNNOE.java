import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

public class OverrideNNOE {
  static class Super {
    @Nullable Object f;

    void call() {}
  }

  static class Sub extends Super {
    @Override
    @RequiresNonNull("f")
    // :: error: (contracts.precondition.override)
    void call() {
      f.hashCode();
    }
  }

  public static void main(String[] args) {
    Super s = new Sub();
    s.call();
  }
}
