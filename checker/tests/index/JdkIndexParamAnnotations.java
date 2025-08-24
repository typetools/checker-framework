import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class JdkIndexParamAnnotations {

  // void get_needs_upper(List<String> xs, int i) {
  //   if (0 <= i) {
  //     // :: error: (argument)
  //     xs.get(i);
  //   }
  // }

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

  void subList_needs_upper(ArrayList<String> xs, int from, int to) {
    if (0 <= from && from <= to) {
      // :: error: (argument)
      xs.subList(from, to);
    }
  }

  // void subList_view_needs_high(ArrayList<String> xs, int i, int k) {
  //   if (0 <= i && i <= xs.size()) {
  //     List<String> view = xs.subList(0, i);
  //     if (0 <= k && k < view.size()) {
  //     // :: error: (argument)
  //       view.add(k, "x");
  //     }
  //   }
  // }

  void subList_view_needs_high(ArrayList<String> xs, int i, int k) {
    if (0 <= i && i <= xs.size()) {
      // :: error: (argument)
      List<String> view = xs.subList(0, i);
      if (0 <= k && k < view.size()) {
        view.add(k, "x");
      }
    }
  }

  void remove_needs_upper_ArrayList(ArrayList<String> xs, int i) {
    if (0 <= i) {
      // :: error: (argument)
      xs.remove(i);
    }
  }

  void remove_needs_upper_SubList(ArrayList<String> xs, int i) {
    // :: error: (argument)
    List<String> view = xs.subList(0, xs.size());
    if (0 <= i) {
      view.remove(i);
    }
  }

  void remove_needs_upper_Vector(Vector<String> xs, int i) {
    if (0 <= i) {
      // :: error: (argument)
      xs.remove(i);
    }
  }

  void remove_needs_upper_COWAL(CopyOnWriteArrayList<String> xs, int i) {
    if (0 <= i) {
      // :: error: (argument)
      xs.remove(i);
    }
  }

  // void subList_clear_exercises_removeRange(ArrayList<String> xs, int from, int to) {
  //   if (0 <= from && from <= xs.size() && 0 <= to && to <= xs.size() && from <= to) {
  //     List<String> v = xs.subList(from, to);
  //     v.clear(); // // calls SubList.removeRange(0, v.size()), (positive)
  //   }
  // }

  void addAll_needs_high_ArrayList(ArrayList<String> xs, int i) {
    if (0 <= i && i < xs.size()) {
      // :: error: (argument)
      xs.addAll(i, List.of("x"));
    }
  }

  void addAll_needs_high_LinkedList(LinkedList<String> xs, int i) {
    if (0 <= i && i < xs.size()) {
      // :: error: (argument)
      xs.addAll(i, List.of("x"));
    }
  }

  void addAll_needs_high_Vector(Vector<String> xs, int i) {
    if (0 <= i && i < xs.size()) {
      // :: error: (argument)
      xs.addAll(i, List.of("x"));
    }
  }

  void listIterator_needs_high_LinkedList(LinkedList<String> xs, int i) {
    if (0 <= i && i < xs.size()) {
      // :: error: (argument)
      xs.listIterator(i);
    }
  }

  void listIterator_needs_high_Vector(Vector<String> xs, int i) {
    if (0 <= i && i < xs.size()) {
      // :: error: (argument)
      xs.listIterator(i);
    }
  }

  void listIterator_needs_high_COWAL(java.util.concurrent.CopyOnWriteArrayList<String> xs, int i) {
    if (0 <= i && i < xs.size()) {
      // :: error: (argument)
      xs.listIterator(i);
    }
  }

  void listIterator_needs_high_on_COWSubList(
      java.util.concurrent.CopyOnWriteArrayList<String> xs, int i) {
    // :: error: (argument)
    List<String> view = xs.subList(0, xs.size());
    if (0 <= i && i < view.size()) {
      view.listIterator(i);
    }
  }

  void get_set_needs_upper_LinkedList(LinkedList<String> xs, int i) {
    if (0 <= i) {
      // :: error: (argument)
      xs.get(i);
      // :: error: (argument)
      xs.set(i, "z");
    }
  }

  void subList_needs_bounds_Vector(Vector<String> xs, int from, int to) {
    if (0 <= from && from <= to) {
      // :: error: (argument)
      xs.subList(from, to);
    }
  }

  void subList_needs_bounds_COWAL(
      java.util.concurrent.CopyOnWriteArrayList<String> xs, int from, int to) {
    if (0 <= from && from <= to) {
      // :: error: (argument)
      xs.subList(from, to);
    }
  }

  void listIterator_ok_inclusive_high_ArrayList(ArrayList<String> xs, int i) {
    if (0 <= i && i <= xs.size()) {
      // :: error: (argument)
      xs.listIterator(i);
    }
  }

  // void listIterator_ok_inclusive_high_ArrayList(ArrayList<String> xs, int i) {
  //   if (0 <= i && i <= xs.size()) {
  //     xs.listIterator(i);
  //   }
  // }

  void get_set_needs_upper_Vector(Vector<String> xs, int i) {
    if (0 <= i) {
      // :: error: (argument)
      xs.get(i);
      // :: error: (argument)
      xs.set(i, "z");
    }
  }

  // void subList_needs_bounds_LinkedList(LinkedList<String> xs, int from, int to) {
  //   if (0 <= from && from <= to) {
  //     // :: error: (argument)
  //     xs.subList(from, to);
  //   }
  // }

  // positive test of the subList_needs_bounds_LinkedList
  void subList_ok_LinkedList(LinkedList<String> xs, int from, int to) {
    if (0 <= from && from <= to && to <= xs.size()) {
      xs.subList(from, to);
    }
  }

  // void COWSubList_add_needs_high(CopyOnWriteArrayList<String> xs, int i, int k) {
  //   if (0 <= i && i <= xs.size()) {
  //     List<String> view = xs.subList(0, i);
  //     if (0 <= k) {
  //       // :: error: (argument)
  //       view.add(k, "x");
  //     }
  //   }
  // }

  void COWSubList_add_needs_high(CopyOnWriteArrayList<String> xs, int i, int k) {
    if (0 <= i && i <= xs.size()) {
      // :: error: (argument)
      List<String> view = xs.subList(0, i);
      if (0 <= k) {
        view.add(k, "x");
      }
    }
  }
}
