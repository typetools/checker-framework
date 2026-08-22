// Modifying an object that the method being checked created is not a side effect that is visible
// to the caller, because the object did not exist before the call.

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class FreshlyAllocated {

  static class Box {
    List<String> contents;
    Box nested;

    @SideEffectsOnly("this")
    Box() {}
  }

  @SideEffectsOnly("this")
  void modifiesFreshObject() {
    List<String> fresh = new ArrayList<>();
    fresh.add("x");
  }

  // Assigning to a field of a freshly created object is not visible to the caller either.
  @SideEffectsOnly("this")
  void assignsFieldOfFreshObject() {
    Box fresh = new Box();
    fresh.contents = null;
  }

  // Only the fresh object's own fields are exempt. `fresh.nested` may be an object that existed
  // before the call.
  @SideEffectsOnly("this")
  void assignsFieldOfFieldOfFreshObject(Box other) {
    Box fresh = new Box();
    fresh.nested = other;
    // :: error: (purity.incorrect.sideeffectsonly)
    fresh.nested.contents = null;
  }

  // A variable that is also assigned something other than a `new` expression is not known to hold
  // an object that this method created.
  @SideEffectsOnly("this")
  void reassignedFromParameter(List<String> a) {
    List<String> maybeFresh = new ArrayList<>();
    maybeFresh = a;
    // :: error: (purity.incorrect.sideeffectsonly)
    maybeFresh.add("x");
  }

  // Storing the fresh object into pre-existing state is itself a side effect, which is reported
  // unless the annotation covers it.
  @SideEffectsOnly("this")
  void freshObjectEscapes(List<List<String>> a) {
    List<String> fresh = new ArrayList<>();
    // :: error: (purity.incorrect.sideeffectsonly)
    a.add(fresh);
    fresh.add("x");
  }
}
