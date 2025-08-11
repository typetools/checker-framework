import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import org.checkerframework.checker.index.qual.GrowOnly;
import org.checkerframework.checker.index.qual.Shrinkable;

/**
 * Test file for polymorphic method return types with GrowOnly annotations. Tests that methods
 * returning iterators, sublists, and streams preserve the qualifier of the receiver.
 */
public class GrowOnlyPolymorphicTest {

  void testPolymorphicReturns(
      @GrowOnly List<String> growOnlyList, @Shrinkable List<String> shrinkableList) {
    // Test that iterators preserve qualifiers
    Iterator<String> growOnlyIter = growOnlyList.iterator();
    Iterator<String> shrinkableIter = shrinkableList.iterator();

    // GrowOnly iterator should not allow removal
    growOnlyIter.next();
    // :: error: (method.invocation)
    growOnlyIter.remove();

    // Shrinkable iterator should allow removal
    shrinkableIter.next();
    shrinkableIter.remove();

    // Test subList behavior - should preserve qualifiers
    List<String> growOnlySublist = growOnlyList.subList(0, Math.min(1, growOnlyList.size()));
    List<String> shrinkableSublist = shrinkableList.subList(0, Math.min(1, shrinkableList.size()));

    // GrowOnly sublist should not allow shrinking operations
    // :: error: (method.invocation)
    growOnlySublist.clear();
    // :: error: (method.invocation)
    growOnlySublist.remove("test");

    // Shrinkable sublist should allow shrinking operations
    shrinkableSublist.clear();
    shrinkableSublist.remove("test");

    // Test stream operations preserve qualifiers
    Stream<String> growOnlyStream = growOnlyList.stream();
    Stream<String> shrinkableStream = shrinkableList.stream();

    // Both should allow non-mutating operations
    growOnlyStream.filter(s -> s.length() > 0).count();
    shrinkableStream.filter(s -> s.length() > 0).count();
  }

  void testListIteratorPolymorphism(@GrowOnly List<String> growOnlyList) {
    // Test that listIterator also preserves qualifiers
    java.util.ListIterator<String> listIter = growOnlyList.listIterator();

    // Should allow navigation and addition
    if (listIter.hasNext()) {
      listIter.next();
    }
    listIter.add("newItem"); // Growing is allowed

    if (listIter.hasPrevious()) {
      listIter.previous();
      listIter.set("modified"); // Setting is allowed (doesn't change size)
    }

    if (listIter.hasNext()) {
      listIter.next();
      // :: error: (method.invocation)
      listIter.remove(); // But removal should not be allowed
    }
  }

  void testViewsAndWrappers(@GrowOnly List<String> growOnlyList) {
    // Test various collection views
    List<String> synchronizedList = java.util.Collections.synchronizedList(growOnlyList);
    List<String> unmodifiableList = java.util.Collections.unmodifiableList(growOnlyList);
    List<String> checkedList = java.util.Collections.checkedList(growOnlyList, String.class);

    // These views should preserve the @GrowOnly property
    // :: error: (method.invocation)
    synchronizedList.clear();
    // :: error: (method.invocation)
    checkedList.remove("test");

    // Unmodifiable list should prevent all modifications (this is a special case)
    // The unmodifiable wrapper makes the list immutable, so even add should fail
    // But this depends on how the annotations are set up for Collections.unmodifiableList()
  }

  void testGenericPolymorphism() {
    @GrowOnly List<String> stringList = new @GrowOnly ArrayList<>();

    // Test assignment to more general types
    @GrowOnly List<? extends String> wildcardList = stringList;
    @GrowOnly Collection<String> collection = stringList;
    @GrowOnly Iterable<String> iterable = stringList;

    // All should preserve @GrowOnly restrictions
    // :: error: (method.invocation)
    collection.clear();
    // :: error: (method.invocation)
    collection.remove("test");

    Iterator<String> iter = iterable.iterator();
    iter.next();
    // :: error: (method.invocation)
    iter.remove();
  }

  <T> void testGenericMethodPolymorphism(@GrowOnly List<T> list) {
    // Test that generic methods preserve qualifiers
    Iterator<T> iter = list.iterator();

    if (iter.hasNext()) {
      T item = iter.next();
      // :: error: (method.invocation)
      iter.remove();
    }

    // Test sublist with generic types
    List<T> sublist = list.subList(0, Math.min(1, list.size()));
    // :: error: (method.invocation)
    sublist.clear();

    // Generic wildcards should work correctly
    @GrowOnly List<? extends T> wildcardList = list;
    // :: error: (method.invocation)
    wildcardList.remove(null);
  }

  void testToArrayPolymorphism(@GrowOnly List<String> list) {
    // toArray methods should be allowed (they don't modify the list)
    Object[] objArray = list.toArray();
    String[] strArray = list.toArray(new String[0]);
    String[] strArray2 = list.toArray(String[]::new);

    // But these don't affect the list's mutability restrictions
    // :: error: (method.invocation)
    list.clear();
  }

  void testSpliteratorPolymorphism(@GrowOnly List<String> list) {
    // Spliterator should also preserve qualifiers
    java.util.Spliterator<String> spliterator = list.spliterator();

    // Navigation should be allowed
    spliterator.tryAdvance(System.out::println);
    spliterator.forEachRemaining(System.out::println);

    // But the underlying list should still be protected
    // :: error: (method.invocation)
    list.remove(0);
  }
}
