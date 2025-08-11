import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.checkerframework.checker.index.qual.GrowOnly;

/**
 * Test file for GrowOnly annotation with iterators and modification methods. Tests that
 * iterator-based modifications are properly prevented on @GrowOnly collections.
 */
public class GrowOnlyIteratorTest {

  void testIteratorRemove(@GrowOnly List<String> list) {
    list.add("test"); // Ensure list has content
    Iterator<String> iter = list.iterator();
    iter.next();
    // :: error: (method.invocation)
    iter.remove();
  }

  void testListIteratorRemove(@GrowOnly List<String> list) {
    list.add("test"); // Ensure list has content
    ListIterator<String> listIter = list.listIterator();
    listIter.next();
    // :: error: (method.invocation)
    listIter.remove();
  }

  void testListIteratorSet(@GrowOnly List<String> list) {
    list.add("test"); // Ensure list has content
    ListIterator<String> listIter = list.listIterator();
    listIter.next();
    // This should be allowed - set doesn't change size
    listIter.set("newValue");
  }

  void testListIteratorAdd(@GrowOnly List<String> list) {
    ListIterator<String> listIter = list.listIterator();
    // This should be allowed - add grows the list
    listIter.add("newValue");
  }

  void testRemoveIf(@GrowOnly List<String> list) {
    // :: error: (method.invocation)
    list.removeIf(s -> s.length() > 5);
  }

  void testBulkRemovalMethods(@GrowOnly List<String> list) {
    List<String> toRemove = new ArrayList<>();
    toRemove.add("item1");
    toRemove.add("item2");

    // :: error: (method.invocation)
    list.removeAll(toRemove);

    // :: error: (method.invocation)
    list.retainAll(toRemove);

    // :: error: (method.invocation)
    list.clear();
  }

  void testAllowedModifications(@GrowOnly List<String> list) {
    // These should all be allowed - they don't shrink the list
    list.add("item");
    list.add(0, "item");
    list.addAll(new ArrayList<>());
    list.addAll(0, new ArrayList<>());

    if (!list.isEmpty()) {
      list.set(0, "newValue");
    }

    // Non-mutating operations should be allowed
    list.get(0);
    list.size();
    list.isEmpty();
    list.contains("item");
    list.indexOf("item");
    list.lastIndexOf("item");
  }

  void testArrayListSpecificMethods() {
    @GrowOnly ArrayList<String> arrayList = new @GrowOnly ArrayList<>();
    arrayList.add("item");

    // trimToSize should be allowed - it's optimization, not removal
    arrayList.trimToSize();

    // ensureCapacity should be allowed - it's growth preparation
    arrayList.ensureCapacity(100);
  }

  void testIteratorOnGrowOnlyList() {
    @GrowOnly List<String> list = new @GrowOnly ArrayList<>();
    list.add("item1");
    list.add("item2");

    // Getting iterator should preserve @GrowOnly qualifier
    Iterator<String> iter = list.iterator();

    // Navigation should be allowed
    while (iter.hasNext()) {
      String item = iter.next();
      // But removal should not
      // :: error: (method.invocation)
      iter.remove();
    }
  }

  void testStreamOperations(@GrowOnly List<String> list) {
    // Stream creation should be allowed
    list.stream().forEach(System.out::println);
    list.parallelStream().forEach(System.out::println);

    // Filtering (non-mutating) should be allowed
    list.stream().filter(s -> s.length() > 3).count();

    // Collecting to new list should be allowed
    List<String> filtered = list.stream().filter(s -> s.length() > 3).toList();
  }
}
