// Checking of `@SideEffectsOnly` is unsound in the presence of aliasing.  This file pins down the
// approximation that the checker actually implements, which is described in the manual under
// "Checking @SideEffectsOnly".  If the alias analysis is ever made more precise, these
// expectations should change deliberately, not by accident.

import java.util.List;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class AliasingUnsoundness {

  // Alias sets grow but are never split, so `b` ends up in the same set as `a`, which is listed.
  @SideEffectsOnly("#1")
  void neverSplitsAliasSets(List<String> a, List<String> b) {
    List<String> t = a;
    t = b; // a, b, and t are now all in one alias set
    t.add("x"); // accepted, although it modifies b, which is not listed
  }

  // The body is scanned once in source order, so an alias is visible only after the assignment
  // that creates it.  This is the same method as above with the last two statements swapped.
  @SideEffectsOnly("#1")
  void aliasIsNotVisibleBeforeItIsCreated(List<String> a, List<String> b) {
    List<String> t = b;
    // :: error: (purity.incorrect.sideeffectsonly)
    t.add("x");
    t = a;
  }

  // An alias created outside the method body is invisible to the checker.
  @SideEffectsOnly("#1")
  void aliasCreatedElsewhere(List<String> a, List<String> b) {
    a.add("x"); // OK: `a` is listed
    // Nothing here reveals that the caller may have passed the same list as both arguments, so
    // the checker does not report that this method can modify `b`.
  }
}
