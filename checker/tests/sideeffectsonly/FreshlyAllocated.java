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

  // The value of a `new` expression is also an object that this method created, whether the call
  // is made on it or it is passed to the call.
  @SideEffectsOnly("this")
  void modifiesNewObject() {
    new ArrayList<String>().add("x");
  }

  @SideEffectsOnly("this")
  void passesNewObject() {
    modifies(new ArrayList<String>());
  }

  @SideEffectsOnly("#1")
  void modifies(List<String> l) {
    l.add("x");
  }

  // So is an array that a `new` expression creates, or that the call site creates out of the
  // arguments to a varargs formal parameter.
  @SideEffectsOnly("this")
  void passesNewArray() {
    modifiesArray(new String[1]);
  }

  @SideEffectsOnly("this")
  void passesVarargs() {
    modifiesVarargs("a", "b");
  }

  @SideEffectsOnly("this")
  void passesExistingArray(String[] a) {
    // :: error: (purity.incorrect.sideeffectsonly)
    modifiesVarargs(a);
  }

  @SideEffectsOnly("#1")
  void modifiesArray(String[] a) {
    a[0] = "x";
  }

  @SideEffectsOnly("#1")
  void modifiesVarargs(String... a) {
    a[0] = "x";
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
