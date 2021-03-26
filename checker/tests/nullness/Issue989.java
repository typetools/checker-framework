// Test case for Issue 989:
// https://github.com/typetools/checker-framework/issues/989

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;

interface ListWrapper989a<E> extends List<@NonNull E> {}

interface ListWrapper989b<E> extends Serializable, List<@NonNull E> {}

interface ListWrapper989c<E> extends Collection<@NonNull E>, Serializable, List<@NonNull E> {}

public class Issue989 {
  void usea(ListWrapper989a<Boolean> list) {
    list.get(0);
  }

  void useb(ListWrapper989b<Boolean> list) {
    list.get(0);
  }

  void usec(ListWrapper989c<Boolean> list) {
    list.get(0);
  }
}
