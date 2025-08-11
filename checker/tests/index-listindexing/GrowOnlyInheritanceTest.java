import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import org.checkerframework.checker.index.qual.GrowOnly;

/**
 * Test file for complex inheritance hierarchies with GrowOnly annotations. Tests that @GrowOnly
 * restrictions are preserved across class hierarchies and polymorphic assignments.
 */
public class GrowOnlyInheritanceTest {

  void testComplexHierarchies() {
    @GrowOnly ArrayList<String> arrayList = new @GrowOnly ArrayList<>();
    @GrowOnly AbstractList<String> abstractList = arrayList;
    @GrowOnly AbstractCollection<String> abstractCollection = abstractList;
    @GrowOnly Collection<String> collection = abstractCollection;
    @GrowOnly Iterable<String> iterable = collection;

    // Each level should preserve @GrowOnly and allow growth
    arrayList.add("item1");
    abstractList.add("item2");
    collection.add("item3");

    // Each level should prevent shrinking operations
    // :: error: (method.invocation)
    collection.remove("test");
    // :: error: (method.invocation)
    collection.clear();
    // :: error: (method.invocation)
    abstractCollection.remove("test");
    // :: error: (method.invocation)
    abstractCollection.clear();
    // :: error: (method.invocation)
    abstractList.remove(0);
    // :: error: (method.invocation)
    abstractList.clear();
    // :: error: (method.invocation)
    arrayList.remove("test");
    // :: error: (method.invocation)
    arrayList.clear();

    // Iterator from any level should preserve restrictions
    java.util.Iterator<String> iter = iterable.iterator();
    iter.next();
    // :: error: (method.invocation)
    iter.remove();
  }

  void testLinkedListHierarchy() {
    @GrowOnly LinkedList<String> linkedList = new @GrowOnly LinkedList<>();
    @GrowOnly AbstractList<String> abstractList = linkedList;
    @GrowOnly List<String> list = linkedList;
    @GrowOnly Queue<String> queue = linkedList;
    @GrowOnly Deque<String> deque = linkedList;
    @GrowOnly Collection<String> collection = linkedList;

    // All should allow growth
    linkedList.add("item1");
    list.add("item2");
    queue.offer("item3");
    deque.addFirst("item4");
    deque.addLast("item5");
    collection.add("item6");

    // None should allow shrinking via any interface
    // :: error: (method.invocation)
    linkedList.removeFirst();
    // :: error: (method.invocation)
    linkedList.removeLast();
    // :: error: (method.invocation)
    linkedList.poll();
    // :: error: (method.invocation)
    list.remove(0);
    // :: error: (method.invocation)
    queue.poll();
    // :: error: (method.invocation)
    queue.remove();
    // :: error: (method.invocation)
    deque.removeFirst();
    // :: error: (method.invocation)
    deque.removeLast();
    // :: error: (method.invocation)
    deque.poll();
    // :: error: (method.invocation)
    deque.pollFirst();
    // :: error: (method.invocation)
    deque.pollLast();
    // :: error: (method.invocation)
    deque.pop();
    // :: error: (method.invocation)
    collection.remove("item1");
    // :: error: (method.invocation)
    collection.clear();
  }

  void testMethodOverridingScenarios(@GrowOnly List<String> list) {
    // Test that method overriding preserves the contract
    // This tests the MethodVisitor's handling of overridden methods

    if (list instanceof ArrayList) {
      @GrowOnly ArrayList<String> arrayList = (@GrowOnly ArrayList<String>) list;
      // ArrayList's specific methods should maintain restrictions
      // :: error: (method.invocation)
      arrayList.removeRange(0, 1); // Protected method that removes elements
    }

    if (list instanceof LinkedList) {
      @GrowOnly LinkedList<String> linkedList = (@GrowOnly LinkedList<String>) list;
      // LinkedList's specific methods should maintain restrictions
      // :: error: (method.invocation)
      linkedList.removeFirst();
      // :: error: (method.invocation)
      linkedList.removeLast();
    }
  }

  void testGenericInheritance() {
    // Test inheritance with generic type parameters
    @GrowOnly List<Number> numberList = new @GrowOnly ArrayList<Number>();
    @GrowOnly Collection<Number> numberCollection = numberList;
    @GrowOnly Iterable<Number> numberIterable = numberCollection;

    // Should allow additions of appropriate types
    numberList.add(42);
    numberList.add(3.14);
    numberCollection.add(100L);

    // Should prevent removal at any level
    // :: error: (method.invocation)
    numberList.remove(42);
    // :: error: (method.invocation)
    numberCollection.remove(3.14);

    // Test wildcards in inheritance
    @GrowOnly List<? extends Number> wildcardList = numberList;
    @GrowOnly Collection<? extends Number> wildcardCollection = wildcardList;

    // Should still prevent removal
    // :: error: (method.invocation)
    wildcardList.clear();
    // :: error: (method.invocation)
    wildcardCollection.clear();
  }

  void testInterfaceMethodConflicts() {
    @GrowOnly LinkedList<String> list = new @GrowOnly LinkedList<>();

    // Test that methods with same name from different interfaces
    // are handled correctly

    // Queue.remove() vs Collection.remove(Object)
    @GrowOnly Queue<String> queue = list;
    @GrowOnly Collection<String> collection = list;

    list.offer("item");

    // Both forms of remove should be prevented
    // :: error: (method.invocation)
    queue.remove(); // Queue.remove() - removes head
    // :: error: (method.invocation)
    collection.remove("item"); // Collection.remove(Object)

    // Similar test for poll vs other methods
    // :: error: (method.invocation)
    queue.poll();
  }

  static class CustomList<T> extends ArrayList<T> {
    // Custom list implementation to test inheritance

    public boolean customRemove(T item) {
      return super.remove(item);
    }

    public void customClear() {
      super.clear();
    }
  }

  void testCustomImplementations() {
    @GrowOnly CustomList<String> customList = new @GrowOnly CustomList<>();
    @GrowOnly List<String> listView = customList;

    // Should allow growth
    customList.add("item");
    listView.add("item2");

    // Standard methods should be restricted
    // :: error: (method.invocation)
    customList.remove("item");
    // :: error: (method.invocation)
    customList.clear();
    // :: error: (method.invocation)
    listView.remove(0);
    // :: error: (method.invocation)
    listView.clear();

    // Custom methods that delegate to restricted methods should also be restricted
    // (This depends on how the type system handles method delegation)
    // :: error: (method.invocation)
    customList.customRemove("item");
    // :: error: (method.invocation)
    customList.customClear();
  }

  void testCovariantReturnTypes(@GrowOnly List<String> list) {
    // Test methods with covariant return types
    @GrowOnly List<String> subList = list.subList(0, Math.min(1, list.size()));

    // The sublist should maintain the same restrictions
    subList.add("newItem");
    // :: error: (method.invocation)
    subList.clear();
    // :: error: (method.invocation)
    subList.remove(0);

    // Test with clone() if the implementation supports it
    if (list instanceof ArrayList) {
      @GrowOnly ArrayList<String> originalArrayList = (@GrowOnly ArrayList<String>) list;
      // Clone should preserve the annotation (if properly implemented)
      @GrowOnly ArrayList<String> cloned = (@GrowOnly ArrayList<String>) originalArrayList.clone();

      cloned.add("item");
      // :: error: (method.invocation)
      cloned.clear();
    }
  }

  void testBridgeMethods(@GrowOnly List<String> list) {
    // Test scenarios that might involve bridge methods in generics
    @GrowOnly List<Object> objectList = new @GrowOnly ArrayList<>();
    objectList.add("string");
    objectList.add(42);

    // Should not allow removal regardless of the actual type
    // :: error: (method.invocation)
    objectList.remove("string");
    // :: error: (method.invocation)
    objectList.remove(42);

    // Test assignment to raw types (if supported)
    @SuppressWarnings("rawtypes")
    @GrowOnly
    List rawList = objectList;

    // Should still prevent removal
    // :: error: (method.invocation)
    rawList.clear();
  }
}
