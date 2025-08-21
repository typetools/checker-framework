import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class JdkIndexParamAnnotationsNegative {

  // ----- get/set/remove need 0 <= i < size  -----
  void get_needs_upper(List<String> xs, int i) {
    if (0 <= i) {
      xs.get(i); // :: error: (list.access.unsafe.high)
    }
  }

  void set_needs_upper(ArrayList<String> xs, int i) {
    if (0 <= i) {
      xs.set(i, "a"); // :: error: (list.access.unsafe.high)
    }
  }

  // ----- add(index, …) / addAll(index, …) / listIterator(index) need 0 <= i <= size -----
  void add_at_needs_high(Vector<String> xs, int i) {
    if (0 <= i && i < xs.size()) {
      xs.add(i, "v"); // :: error: (list.access.unsafe.high)
    }
  }

  void addAll_at_needs_high(CopyOnWriteArrayList<String> xs, int i) {
    if (0 <= i && i < xs.size()) {
      xs.addAll(i, List.of("x")); // :: error: (list.access.unsafe.high)
    }
  }

  void listIterator_needs_high(ArrayList<String> xs, int i) {
    if (0 <= i && i < xs.size()) {
      xs.listIterator(i); // :: error: (list.access.unsafe.high)
    }
  }

  // ----- subList(from,to) needs 0 <= from <= size && 0 <= to <= size -----
  void subList_needs_upper(ArrayList<String> xs, int from, int to) {
    if (0 <= from && from <= to) {
      xs.subList(from, to); // :: error: (list.access.unsafe.high)  (missing upper bound(s))
    }
  }

  // ----- SubList view enforces the same contracts -----
  void subList_view_needs_high(ArrayList<String> xs, int i, int k) {
    if (0 <= i && i <= xs.size()) {
      List<String> view = xs.subList(0, i);
      if (0 <= k && k < view.size()) {
        view.add(k, "x"); // :: error: (list.access.unsafe.high)  (needs k <= size)
      }
    }
  }
}
