import java.util.ArrayList;
import java.util.List;

public class WildcardIterable {
  private static <T> List<T> catListAndIterable(
      final List<T> list, final Iterable<? extends T> iterable) {
    final List<T> newList = new ArrayList<>();

    for (T listObject : list) {
      newList.add(listObject);
    }

    for (T iterObject : iterable) {
      newList.add(iterObject);
    }

    return newList;
  }
}
