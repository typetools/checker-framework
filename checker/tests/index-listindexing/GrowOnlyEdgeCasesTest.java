import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Stream;
import org.checkerframework.checker.index.qual.GrowOnly;

/**
 * Test file for edge cases and collection utilities with GrowOnly annotations. Tests boundary
 * conditions, empty collections, null handling, and Collections utility methods.
 */
public class GrowOnlyEdgeCasesTest {

  void testEmptyCollections() {
    @GrowOnly List<String> emptyList = new @GrowOnly ArrayList<>();

    // Operations on empty collections should behave correctly
    boolean isEmpty = emptyList.isEmpty();
    int size = emptyList.size();

    // These should not throw exceptions but should maintain restrictions
    // :: error: (method.invocation)
    emptyList.clear(); // Even clearing an empty list should be restricted

    // Adding to empty list should work
    emptyList.add("first");

    // Now it's not empty, but restrictions still apply
    // :: error: (method.invocation)
    emptyList.remove("first");

    // Iterator on empty list should work
    @GrowOnly List<String> anotherEmpty = new @GrowOnly ArrayList<>();
    Iterator<String> emptyIter = anotherEmpty.iterator();
    boolean hasNext = emptyIter.hasNext(); // Should be false

    // But if we somehow get to remove(), it should still be restricted
    if (hasNext) { // This won't execute, but tests the type system
      // :: error: (method.invocation)
      emptyIter.remove();
    }
  }

  void testNullHandling() {
    @GrowOnly List<String> list = new @GrowOnly ArrayList<>();

    // Adding null should be allowed if the generic type permits it
    list.add(null);
    list.add("item");
    list.add(null);

    // Checking for null should be allowed
    boolean containsNull = list.contains(null);
    int nullIndex = list.indexOf(null);
    int lastNullIndex = list.lastIndexOf(null);

    // But removing null should still be restricted
    // :: error: (method.invocation)
    list.remove(null);

    // removeAll with null collection should be restricted
    try {
      // :: error: (method.invocation)
      list.removeAll(null); // This would throw NPE anyway, but should be caught by type system
    } catch (NullPointerException e) {
      // Expected
    }

    // Set to null should be allowed (doesn't change size)
    if (!list.isEmpty()) {
      list.set(0, null);
    }
  }

  void testBoundaryIndices() {
    @GrowOnly List<String> list = new @GrowOnly ArrayList<>();
    list.add("item0");
    list.add("item1");
    list.add("item2");

    // Valid index operations should work
    String first = list.get(0);
    String last = list.get(list.size() - 1);

    // Set at valid indices should work (doesn't change size)
    list.set(0, "newItem0");
    list.set(list.size() - 1, "newItemLast");

    // Remove at any valid index should be restricted
    // :: error: (method.invocation)
    list.remove(0);
    // :: error: (method.invocation)
    list.remove(list.size() - 1);

    // SubList with boundary indices
    List<String> subList = list.subList(0, 1);
    List<String> fullSubList = list.subList(0, list.size());
    List<String> emptySubList = list.subList(1, 1);

    // All sublists should preserve restrictions
    // :: error: (method.invocation)
    subList.clear();
    // :: error: (method.invocation)
    fullSubList.clear();
    // :: error: (method.invocation)
    emptySubList.clear();
  }

  void testCollectionsUtilities() {
    @GrowOnly List<String> originalList = new @GrowOnly ArrayList<>();
    originalList.add("c");
    originalList.add("a");
    originalList.add("b");

    // Non-mutating Collections utilities should work
    String min = Collections.min(originalList);
    String max = Collections.max(originalList);
    int frequency = Collections.frequency(originalList, "a");
    boolean disjoint = Collections.disjoint(originalList, Collections.singletonList("x"));

    // Sort should be allowed (doesn't change size, though it modifies order)
    Collections.sort(originalList);
    Collections.sort(originalList, Comparator.reverseOrder());

    // Reverse should be allowed (doesn't change size)
    Collections.reverse(originalList);

    // Shuffle should be allowed (doesn't change size)
    Collections.shuffle(originalList);

    // rotate should be allowed (doesn't change size)
    Collections.rotate(originalList, 1);

    // swap should be allowed (doesn't change size)
    Collections.swap(originalList, 0, 1);

    // fill should be allowed (doesn't change size)
    Collections.fill(originalList, "x");

    // copy should be allowed (doesn't change size, requires same size lists)
    @GrowOnly List<String> destList = new @GrowOnly ArrayList<>(originalList);
    Collections.copy(destList, originalList);

    // replaceAll should be allowed (doesn't change size)
    Collections.replaceAll(originalList, "x", "y");
  }

  void testCollectionsWrappers(@GrowOnly List<String> originalList) {
    originalList.add("item1");
    originalList.add("item2");

    // Test various wrapper types
    List<String> synchronizedList = Collections.synchronizedList(originalList);
    List<String> unmodifiableList = Collections.unmodifiableList(originalList);
    List<String> checkedList = Collections.checkedList(originalList, String.class);

    // Synchronized and checked lists should preserve @GrowOnly restrictions
    // :: error: (method.invocation)
    synchronizedList.clear();
    // :: error: (method.invocation)
    synchronizedList.remove("item1");
    // :: error: (method.invocation)
    checkedList.clear();
    // :: error: (method.invocation)
    checkedList.remove("item1");

    // But they should allow growth
    synchronizedList.add("item3");
    checkedList.add("item4");

    // Unmodifiable list should prevent all modifications
    try {
      unmodifiableList.add("item5"); // Should throw UnsupportedOperationException
    } catch (UnsupportedOperationException e) {
      // Expected
    }

    try {
      unmodifiableList.clear(); // Should throw UnsupportedOperationException
    } catch (UnsupportedOperationException e) {
      // Expected
    }
  }

  void testStreamAPIIntegration(@GrowOnly List<String> list) {
    list.add("apple");
    list.add("banana");
    list.add("cherry");

    // Stream creation should be allowed
    Stream<String> stream = list.stream();
    Stream<String> parallelStream = list.parallelStream();

    // Non-mutating stream operations should work
    long count = stream.filter(s -> s.startsWith("a")).count();
    List<String> filtered = parallelStream.filter(s -> s.length() > 5).toList();

    // Collecting to new collections should work
    List<String> collected =
        list.stream().map(String::toUpperCase).collect(java.util.stream.Collectors.toList());

    // The original list should still maintain its restrictions
    // :: error: (method.invocation)
    list.clear();
    // :: error: (method.invocation)
    list.remove("apple");

    // New collections from streams should not automatically inherit @GrowOnly
    // (unless specifically annotated)
    collected.clear(); // This should be allowed
    filtered.clear(); // This should be allowed
  }

  void testIteratorEdgeCases(@GrowOnly List<String> list) {
    list.add("item1");
    list.add("item2");
    list.add("item3");

    // Test iterator at boundaries
    Iterator<String> iter = list.iterator();

    // Multiple hasNext() calls should not affect remove() restriction
    iter.hasNext();
    iter.hasNext();
    iter.next();
    iter.hasNext();
    // :: error: (method.invocation)
    iter.remove();

    // ListIterator edge cases
    ListIterator<String> listIter = list.listIterator();

    // Navigation in both directions
    listIter.next();
    listIter.previous();
    listIter.next();

    // Remove should be restricted regardless of navigation pattern
    // :: error: (method.invocation)
    listIter.remove();

    // Add should be allowed
    listIter.add("newItem");

    // Set should be allowed (doesn't change size)
    listIter.next();
    listIter.set("modifiedItem");

    // But remove should still be restricted
    // :: error: (method.invocation)
    listIter.remove();
  }

  void testConcurrentModification(@GrowOnly List<String> list) {
    list.add("item1");
    list.add("item2");
    list.add("item3");

    Iterator<String> iter = list.iterator();

    // Adding to the list while iterating (this would cause ConcurrentModificationException)
    // But the type system should still prevent remove() on the iterator
    list.add("item4");

    try {
      iter.next();
      // :: error: (method.invocation)
      iter.remove(); // Should be restricted by type system before runtime exception
    } catch (java.util.ConcurrentModificationException e) {
      // Expected runtime behavior, but type system should catch it first
    }
  }

  void testSubListEdgeCases(@GrowOnly List<String> list) {
    list.add("item1");
    list.add("item2");
    list.add("item3");
    list.add("item4");

    // Various sublist ranges
    List<String> fullSubList = list.subList(0, list.size());
    List<String> partialSubList = list.subList(1, 3);
    List<String> emptySubList = list.subList(2, 2);
    List<String> singleSubList = list.subList(1, 2);

    // All should allow growth
    fullSubList.add("newItem1");
    partialSubList.add("newItem2");
    emptySubList.add("newItem3");
    singleSubList.add("newItem4");

    // None should allow shrinking
    // :: error: (method.invocation)
    fullSubList.clear();
    // :: error: (method.invocation)
    partialSubList.remove(0);
    // :: error: (method.invocation)
    emptySubList.clear();
    // :: error: (method.invocation)
    singleSubList.clear();

    // Nested sublists
    List<String> nestedSubList = partialSubList.subList(0, 1);
    nestedSubList.add("nested");
    // :: error: (method.invocation)
    nestedSubList.clear();
  }
}
