import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@SuppressWarnings({"unchecked", "all"}) // just check for crashes.
public class Issue5436 {
  static class BoundedWindow {}

  static class Accessor<K> {
    public Accessor(Supplier<BoundedWindow> s) {}

    Accessor() {}
  }

  private final Accessor<?> stateAccessor;
  private List<BoundedWindow> currentWindows;

  Issue5436() {
    currentWindows = new ArrayList<>();
    stateAccessor = new Accessor(() -> currentWindows);
  }

  private Object getCurrentKey() {
    return new Object();
  }
}
