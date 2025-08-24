import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class JdkIndexParamAnnotations {

  // ----- get/set/remove need 0 <= i < size  -----
  void get_needs_upper(List<String> xs, int i) {
    if (0 <= i) {
      xs.get(i);
    }
  }

  void set_needs_upper(ArrayList<String> xs, int i) {
    if (0 <= i) {
      // :: error: (argument)
      xs.set(i, "a");
    }
  }

  // ----- add(index, …) / addAll(index, …) / listIterator(index) need 0 <= i <= size -----
  void add_at_needs_high(Vector<String> xs, int i) {
    if (0 <= i && i < xs.size()) {
      // :: error: (argument)
      xs.add(i, "v");
    }
  }

  void addAll_at_needs_high(CopyOnWriteArrayList<String> xs, int i) {
    if (0 <= i && i < xs.size()) {
      // :: error: (argument)
      xs.addAll(i, List.of("x"));
    }
  }

  void listIterator_needs_high(ArrayList<String> xs, int i) {
    if (0 <= i && i < xs.size()) {
      // :: error: (argument)
      xs.listIterator(i);
    }
  }

  // ----- subList(from,to) needs 0 <= from <= size && 0 <= to <= size -----
  void subList_needs_upper(ArrayList<String> xs, int from, int to) {
    if (0 <= from && from <= to) {
      // :: error: (argument)
      xs.subList(from, to);
    }
  }

  // ----- SubList view enforces the same contracts -----
  void subList_view_needs_high(ArrayList<String> xs, int i, int k) {
    if (0 <= i && i <= xs.size()) {
      // :: error: (argument)
      List<String> view = xs.subList(0, i);
      if (0 <= k && k < view.size()) {
        view.add(k, "x");
      }
    }
  }
}
