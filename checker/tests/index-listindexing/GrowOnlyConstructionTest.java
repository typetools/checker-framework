import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import org.checkerframework.checker.index.qual.GrowOnly;
import org.checkerframework.checker.index.qual.Shrinkable;
import org.checkerframework.checker.index.qual.UnshrinkableRef;

/**
 * Test file for collection construction and assignment with GrowOnly annotations. Tests various
 * ways of creating and assigning annotated collections.
 */
public class GrowOnlyConstructionTest {

  void testCollectionConstruction() {
    // Test various construction patterns
    @GrowOnly List<String> list1 = new @GrowOnly ArrayList<>();
    @GrowOnly List<String> list2 = new @GrowOnly LinkedList<>();
    @GrowOnly List<String> list3 = new @GrowOnly Vector<>();

    // Test construction with initial capacity
    @GrowOnly List<String> list4 = new @GrowOnly ArrayList<>(10);
    @GrowOnly List<String> list5 = new @GrowOnly Vector<>(10);

    // All should allow growth
    list1.add("item");
    list2.add("item");
    list3.add("item");
    list4.add("item");
    list5.add("item");

    // None should allow shrinking
    // :: error: (method.invocation)
    list1.clear();
    // :: error: (method.invocation)
    list2.remove("item");
    // :: error: (method.invocation)
    list3.clear();
    // :: error: (method.invocation)
    list4.remove(0);
    // :: error: (method.invocation)
    list5.clear();
  }

  void testConstructionFromOtherCollections() {
    List<String> sourceList = Arrays.asList("a", "b", "c");

    // Construction from other collections
    @GrowOnly List<String> copy1 = new @GrowOnly ArrayList<>(sourceList);
    @GrowOnly List<String> copy2 = new @GrowOnly LinkedList<>(sourceList);
    @GrowOnly List<String> copy3 = new @GrowOnly Vector<>(sourceList);

    // Should allow growth
    copy1.add("d");
    copy2.add("d");
    copy3.add("d");

    // Should not allow shrinking
    // :: error: (method.invocation)
    copy1.clear();
    // :: error: (method.invocation)
    copy2.remove("a");
    // :: error: (method.invocation)
    copy3.removeAll(sourceList);
  }

  void testArraysAsList() {
    // Test Arrays.asList - this creates a fixed-size list
    List<String> fixedList = Arrays.asList("a", "b", "c");

    // This assignment might need special handling depending on implementation
    // Arrays.asList returns a list that doesn't support add/remove operations
    // so the annotation behavior might be different
    @GrowOnly List<String> growOnlyFixed = new @GrowOnly ArrayList<>(fixedList);

    // The copy should allow growth
    growOnlyFixed.add("d");

    // But not shrinking
    // :: error: (method.invocation)
    growOnlyFixed.clear();
  }

  void testAssignmentCompatibility() {
    @GrowOnly List<String> growOnlyList = new @GrowOnly ArrayList<>();

    // Test assignment to supertype references
    @UnshrinkableRef List<String> unshrinkableRef = growOnlyList; // Should work

    // Test that unshrinkableRef still can't be used to shrink
    // (this tests that the runtime object retains its restrictions)
    // :: error: (method.invocation)
    unshrinkableRef.clear();

    // Test assignment to incompatible types (should fail)
    // :: error: (assignment)
    @Shrinkable List<String> shrinkableRef = growOnlyList;
  }

  void testPolymorphicAssignment() {
    @GrowOnly ArrayList<String> growOnlyArrayList = new @GrowOnly ArrayList<>();
    @GrowOnly LinkedList<String> growOnlyLinkedList = new @GrowOnly LinkedList<>();

    // Assignment to less specific types should work
    @GrowOnly List<String> list1 = growOnlyArrayList;
    @GrowOnly List<String> list2 = growOnlyLinkedList;

    // Assignment to collection interfaces should work
    @GrowOnly java.util.Collection<String> coll1 = growOnlyArrayList;
    @GrowOnly java.util.Collection<String> coll2 = growOnlyLinkedList;

    // All should preserve restrictions
    // :: error: (method.invocation)
    list1.clear();
    // :: error: (method.invocation)
    list2.clear();
    // :: error: (method.invocation)
    coll1.clear();
    // :: error: (method.invocation)
    coll2.clear();
  }

  void testGenericTypeParameters() {
    // Test with different generic type parameters
    @GrowOnly List<Integer> intList = new @GrowOnly ArrayList<>();
    @GrowOnly List<Double> doubleList = new @GrowOnly ArrayList<>();
    @GrowOnly List<Object> objectList = new @GrowOnly ArrayList<>();

    // Test assignment with generic bounds
    @GrowOnly List<Number> numberList1 = new @GrowOnly ArrayList<Integer>();
    // :: error: (assignment)
    @GrowOnly List<Integer> intList2 = numberList1; // Should fail

    // But this should work
    @GrowOnly List<? extends Number> wildcardList = intList;

    // All should maintain restrictions
    // :: error: (method.invocation)
    wildcardList.clear();
  }

  void testNestedGenerics() {
    // Test with nested generic types
    @GrowOnly List<List<String>> nestedList = new @GrowOnly ArrayList<>();

    // Should allow adding lists
    nestedList.add(new ArrayList<>());

    // Should not allow removal
    // :: error: (method.invocation)
    nestedList.clear();
    // :: error: (method.invocation)
    nestedList.remove(0);

    // Inner lists should not inherit the @GrowOnly annotation automatically
    if (!nestedList.isEmpty()) {
      List<String> innerList = nestedList.get(0);
      innerList.clear(); // This should be allowed unless innerList is also @GrowOnly
    }
  }

  void testStaticFactoryMethods() {
    // Test static factory methods that create lists
    @GrowOnly List<String> emptyList = new @GrowOnly ArrayList<>(java.util.Collections.emptyList());
    @GrowOnly
    List<String> singletonCopy =
        new @GrowOnly ArrayList<>(java.util.Collections.singletonList("item"));

    // These should work as normal @GrowOnly lists
    emptyList.add("item");
    singletonCopy.add("item2");

    // :: error: (method.invocation)
    emptyList.clear();
    // :: error: (method.invocation)
    singletonCopy.clear();
  }

  void testNullHandling() {
    @GrowOnly List<String> list = new @GrowOnly ArrayList<>();

    // Should allow adding null if the generic type permits it
    list.add(null);

    // Should not allow removal even of null
    // :: error: (method.invocation)
    list.remove(null);

    // Checking for null should be allowed
    boolean containsNull = list.contains(null);
    int nullIndex = list.indexOf(null);
  }
}
