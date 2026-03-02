import org.checkerframework.checker.index.qual.BottomGrowShrink;
import org.checkerframework.checker.index.qual.CanShrink;
import org.checkerframework.checker.index.qual.GrowOnly;
import org.checkerframework.checker.index.qual.UncheckedCanShrink;
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
   * @param canShrink A reference that can be used to shrink the collection.
   * @param uncheckedCanShrink A canShrink reference that is no longer index-safe.
   * @param bottom The bottom type, which is a subtype of all others.
   */
  void testHierarchy(
      @UnshrinkableRef Object unshrinkableRef,
      @GrowOnly Object growOnly,
      @CanShrink Object canShrink,
      @UncheckedCanShrink Object uncheckedCanShrink,
      @BottomGrowShrink Object bottom) {

    // Assignments to @UnshrinkableRef (Top type)
    @UnshrinkableRef Object a1 = unshrinkableRef;
    @UnshrinkableRef Object a2 = growOnly;
    @UnshrinkableRef Object a3 = canShrink;
    @UnshrinkableRef Object a4 = uncheckedCanShrink;
    @UnshrinkableRef Object a5 = bottom;

    // Assignments to @GrowOnly
    // :: error: (assignment)
    @GrowOnly Object b1 = unshrinkableRef; // ERROR: Supertype to subtype
    @GrowOnly Object b2 = growOnly;
    // :: error: (assignment)
    @GrowOnly Object b3 = canShrink; // ERROR: Sibling types
    // :: error: (assignment)
    @GrowOnly Object b4 = uncheckedCanShrink; // ERROR: Sibling types
    @GrowOnly Object b5 = bottom;

    // Assignments to @CanShrink
    // :: error: (assignment)
    @CanShrink Object c1 = unshrinkableRef; // ERROR: Supertype to subtype
    // :: error: (assignment)
    @CanShrink Object c2 = growOnly; // ERROR: Sibling types
    @CanShrink Object c3 = canShrink;
    @CanShrink Object c4 = uncheckedCanShrink; // Subtype to supertype
    @CanShrink Object c5 = bottom;

    // Assignments to @UncheckedCanShrink
    // :: error: (assignment)
    @UncheckedCanShrink Object d1 = unshrinkableRef; // ERROR: Supertype to subtype
    // :: error: (assignment)
    @UncheckedCanShrink Object d2 = growOnly; // ERROR: Sibling types
    // :: error: (assignment)
    @UncheckedCanShrink Object d3 = canShrink; // ERROR: Supertype to subtype
    @UncheckedCanShrink Object d4 = uncheckedCanShrink;
    @UncheckedCanShrink Object d5 = bottom;

    // Assignments to @BottomGrowShrink
    // :: error: (assignment)
    @BottomGrowShrink Object e1 = unshrinkableRef; // ERROR
    // :: error: (assignment)
    @BottomGrowShrink Object e2 = growOnly; // ERROR
    // :: error: (assignment)
    @BottomGrowShrink Object e3 = canShrink; // ERROR
    // :: error: (assignment)
    @BottomGrowShrink Object e4 = uncheckedCanShrink; // ERROR
    @BottomGrowShrink Object e5 = bottom;
  }
}
