import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public class CapturedWildcards {
  abstract static class MyClass {
    abstract boolean contains(MyClass other);
  }

  public boolean pass(List<? extends @Nullable MyClass> list, MyClass other) {
    return list.stream().anyMatch(je -> je != null && je.contains(other));
  }

  public boolean fail(List<? extends @Nullable MyClass> list, MyClass other) {
    // :: error: (dereference.of.nullable)
    return list.stream().anyMatch(je -> je.contains(other));
  }
}
