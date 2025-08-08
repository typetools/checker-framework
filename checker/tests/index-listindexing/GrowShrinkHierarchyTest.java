import org.checkerframework.checker.index.qual.BottomGrowShrink;
import org.checkerframework.checker.index.qual.GrowOnly;
import org.checkerframework.checker.index.qual.Shrinkable;
import org.checkerframework.checker.index.qual.UncheckedShrinkable;
import org.checkerframework.checker.index.qual.UnshrinkableRef;

/**
 * Test file for the GrowShrink type hierarchy. This class contains a series of assignments that
 * test the relationships between the different qualifiers. It is intended to be run with the
 * Checker Framework's Subtyping Checker.
 */
public class GrowShrinkHierarchyTest {

  /**
   * Method to test the hierarchy using variable assignments.
   *
   * @param unshrinkableRef A reference that cannot be used to shrink the collection. This is the
   *     top type.
   * @param growOnly A reference that can only be used to grow the collection.
   * @param shrinkable A reference that can be used to shrink the collection.
   * @param uncheckedShrinkable A shrinkable reference that is no longer index-safe.
   * @param bottom The bottom type, which is a subtype of all others.
   */
  void testHierarchy(
      @UnshrinkableRef Object unshrinkableRef,
      @GrowOnly Object growOnly,
      @Shrinkable Object shrinkable,
      @UncheckedShrinkable Object uncheckedShrinkable,
      @BottomGrowShrink Object bottom) {

    // Assignments to @UnshrinkableRef (Top type)
    @UnshrinkableRef Object a1 = unshrinkableRef;
    @UnshrinkableRef Object a2 = growOnly;
    @UnshrinkableRef Object a3 = shrinkable;
    @UnshrinkableRef Object a4 = uncheckedShrinkable;
    @UnshrinkableRef Object a5 = bottom;

    // Assignments to @GrowOnly
    // :: error: (assignment)
    @GrowOnly Object b1 = unshrinkableRef; // ERROR: Supertype to subtype
    @GrowOnly Object b2 = growOnly;
    // :: error: (assignment)
    @GrowOnly Object b3 = shrinkable; // ERROR: Sibling types
    // :: error: (assignment)
    @GrowOnly Object b4 = uncheckedShrinkable; // ERROR: Sibling types
    @GrowOnly Object b5 = bottom;

    // Assignments to @Shrinkable
    // :: error: (assignment)
    @Shrinkable Object c1 = unshrinkableRef; // ERROR: Supertype to subtype
    // :: error: (assignment)
    @Shrinkable Object c2 = growOnly; // ERROR: Sibling types
    @Shrinkable Object c3 = shrinkable;
    @Shrinkable Object c4 = uncheckedShrinkable; // Subtype to supertype
    @Shrinkable Object c5 = bottom;

    // Assignments to @UncheckedShrinkable
    // :: error: (assignment)
    @UncheckedShrinkable Object d1 = unshrinkableRef; // ERROR: Supertype to subtype
    // :: error: (assignment)
    @UncheckedShrinkable Object d2 = growOnly; // ERROR: Sibling types
    // :: error: (assignment)
    @UncheckedShrinkable Object d3 = shrinkable; // ERROR: Supertype to subtype
    @UncheckedShrinkable Object d4 = uncheckedShrinkable;
    @UncheckedShrinkable Object d5 = bottom;

    // Assignments to @BottomGrowShrink
    // :: error: (assignment)
    @BottomGrowShrink Object e1 = unshrinkableRef; // ERROR
    // :: error: (assignment)
    @BottomGrowShrink Object e2 = growOnly; // ERROR
    // :: error: (assignment)
    @BottomGrowShrink Object e3 = shrinkable; // ERROR
    // :: error: (assignment)
    @BottomGrowShrink Object e4 = uncheckedShrinkable; // ERROR
    @BottomGrowShrink Object e5 = bottom;
  }
}
