import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class JdkIndexParamPositiveTests {

  // Positive test cases - these should work correctly with proper bounds

  void get_works_with_proper_bounds(ArrayList<String> xs, int i) {
    if (0 <= i && i < xs.size()) {
      String result = xs.get(i); // Should work with @IndexFor
    }
  }

  void set_works_with_proper_bounds(LinkedList<String> xs, int i) {
    if (0 <= i && i < xs.size()) {
      xs.set(i, "new_value"); // Should work with @IndexFor
    }
  }

  void add_works_at_size(Vector<String> xs) {
    xs.add(xs.size(), "append"); // Should work with @IndexOrHigh
  }

  void addAll_works_at_size(ArrayList<String> xs) {
    xs.addAll(xs.size(), List.of("x", "y")); // Should work with @IndexOrHigh
  }

  void listIterator_works_at_size(CopyOnWriteArrayList<String> xs) {
    ListIterator<String> iter = xs.listIterator(xs.size()); // Should work with @IndexOrHigh
  }

  void subList_works_with_proper_bounds(LinkedList<String> xs, int from, int to) {
    if (0 <= from && from <= to && to <= xs.size()) {
      List<String> view = xs.subList(from, to); // Should work with @IndexOrHigh
    }
  }

  void remove_works_with_proper_bounds(Vector<String> xs, int i) {
    if (0 <= i && i < xs.size()) {
      xs.remove(i); // Should work with @IndexFor
    }
  }

  void subList_ok_LinkedList(LinkedList<String> xs, int from, int to) {
    if (0 <= from && from <= to && to <= xs.size()) {
      xs.subList(from, to);
    }
  }

  // Boundary condition tests

  void test_empty_list_operations(ArrayList<String> xs) {
    if (xs.isEmpty()) {
      // These should work on empty list
      xs.add(0, "first"); // add at index 0 when size is 0
      ListIterator<String> iter = xs.listIterator(0); // iterator at start
      List<String> empty_view = xs.subList(0, 0); // empty sublist
    }
  }

  void test_single_element_operations(LinkedList<String> xs) {
    if (xs.size() == 1) {
      String first = xs.get(0); // get the only element
      xs.set(0, "updated"); // replace the only element
      xs.add(1, "second"); // append after the only element
      List<String> single_view = xs.subList(0, 1); // view of single element
      List<String> from_end = xs.subList(1, 1); // empty view from end
    }
  }

  void test_chain_operations(Vector<String> xs) {
    if (!xs.isEmpty()) {
      xs.set(0, "new_first");
      xs.add(xs.size(), "append");
      if (xs.size() >= 2) {
        String second = xs.get(1);
        xs.remove(xs.size() - 1); // remove last element
      }
    }
  }

  void test_sublist_operations(CopyOnWriteArrayList<String> xs) {
    if (xs.size() >= 3) {
      List<String> middle = xs.subList(1, xs.size() - 1);
      if (!middle.isEmpty()) {
        String first_in_middle = middle.get(0);
        middle.add(middle.size(), "append_to_middle");
        ListIterator<String> iter = middle.listIterator(middle.size());
      }
    }
  }
}
