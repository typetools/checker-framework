import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class JdkIndexParamAnnotationsPositive {

  // ----- ArrayList -----
  void arrayList_all(ArrayList<String> xs, int i, int j) {
    if (0 <= i && i < xs.size()) {
      xs.get(i); // @IndexFor(this)
      xs.set(i, "a"); // @IndexFor(this)
      xs.remove(i); // @IndexFor(this)
    }
    if (0 <= j && j <= xs.size()) {
      xs.add(j, "b"); // @IndexOrHigh(this)
      xs.addAll(j, List.of("x", "y")); // @IndexOrHigh(this)
      xs.listIterator(j); // @IndexOrHigh(this)
    }
    if (0 <= i && i <= xs.size() && 0 <= j && j <= xs.size() && i <= j) {
      xs.subList(i, j); // @IndexOrHigh(this), @IndexOrHigh(this)
    }
  }

  // ----- LinkedList (covers AbstractSequentialList overrides) -----
  void linkedList_all(LinkedList<Integer> xs, int i, int j) {
    if (0 <= i && i < xs.size()) {
      xs.get(i);
      xs.set(i, 1);
      xs.remove(i);
    }
    if (0 <= j && j <= xs.size()) {
      xs.add(j, 2);
      xs.addAll(j, List.of(3, 4));
      xs.listIterator(j);
    }
    if (0 <= i && i <= xs.size() && 0 <= j && j <= xs.size() && i <= j) {
      xs.subList(i, j);
    }
  }

  // ----- Vector -----
  void vector_all(Vector<String> xs, int i, int j) {
    if (0 <= i && i < xs.size()) {
      xs.get(i);
      xs.set(i, "v");
      xs.remove(i);
    }
    if (0 <= j && j <= xs.size()) {
      xs.add(j, "w");
      xs.addAll(j, List.of("p", "q"));
      xs.listIterator(j);
    }
    if (0 <= i && i <= xs.size() && 0 <= j && j <= xs.size() && i <= j) {
      xs.subList(i, j);
    }
  }

  // ----- CopyOnWriteArrayList -----
  void cow_all(CopyOnWriteArrayList<String> xs, int i, int j) {
    if (0 <= i && i < xs.size()) {
      xs.get(i);
      xs.set(i, "c");
      xs.remove(i);
    }
    if (0 <= j && j <= xs.size()) {
      xs.add(j, "d");
      xs.addAll(j, List.of("m", "n"));
      xs.listIterator(j);
    }
    if (0 <= i && i <= xs.size() && 0 <= j && j <= xs.size() && i <= j) {
      xs.subList(i, j);
    }
  }

  // ----- AbstractList.SubList coverage via a subList view -----
  void subList_view(ArrayList<String> xs, int i, int j, int k) {
    if (0 <= i && i <= xs.size() && 0 <= j && j <= xs.size() && i <= j) {
      List<String> view = xs.subList(i, j); // ensures a RandomAccessSubList/ SubList
      int len = view.size();
      if (0 <= k && k < len) {
        view.get(k); // SubList#get @IndexFor(this)
        view.set(k, "z"); // SubList#set @IndexFor(this)
        view.remove(k); // SubList#remove @IndexFor(this)
      }
      if (0 <= k && k <= len) {
        view.add(k, "y"); // SubList#add @IndexOrHigh(this)
        view.addAll(k, List.of("r")); // SubList#addAll @IndexOrHigh(this)
        view.listIterator(k); // SubList#listIterator @IndexOrHigh(this)
      }
      if (0 <= k && k <= len) {
        // nested subList exercises AbstractList#subList annotations again
        view.subList(0, k);
      }
    }
  }
}
