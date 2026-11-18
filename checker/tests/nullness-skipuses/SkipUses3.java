// The type arguments of a method reference are not checked if the method is in a skipped class.

import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SkipUses3 {

  interface ListConsumer {
    <T extends @Nullable Object> void use(List<T> list);
  }

  static class SkipMe {
    static <@Nullable U extends @Nullable Object> void addNull(List<U> list) {
      list.add(null);
    }
  }

  static class DontSkip {
    static <@Nullable U extends @Nullable Object> void addNull(List<U> list) {
      list.add(null);
    }
  }

  void methodReferences() {
    ListConsumer skipped = SkipMe::addNull;
    // :: error: [type.argument]
    ListConsumer checked = DontSkip::addNull;
  }
}
