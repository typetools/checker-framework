// A parenthesized assignment target or receiver denotes the same location as the unparenthesized
// one.

import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class ParenthesizedAssignment {

  int f;
  int g;
  ParenthesizedAssignment other;

  @SideEffectsOnly("this")
  ParenthesizedAssignment() {}

  @SideEffectsOnly("this.f")
  void assignsLocal() {
    int x;
    (x) = 1;
  }

  @SideEffectsOnly("this.f")
  void assignsPermittedField() {
    (this.f) = 1;
    (f) = 2;
    (this).f = 3;
  }

  @SideEffectsOnly("this.f")
  void assignsOtherField() {
    // :: error: (purity.incorrect.sideeffectsonly)
    (this.g) = 1;
    // :: error: (purity.incorrect.sideeffectsonly)
    (g) = 2;
    // :: error: (purity.incorrect.sideeffectsonly)
    (this).g = 3;
  }

  @SideEffectsOnly("this.f")
  void assignsFieldOfFreshObject() {
    ParenthesizedAssignment fresh = new ParenthesizedAssignment();
    (fresh).g = 1;
    (fresh.g) = 2;
  }

  @SideEffectsOnly("this.f")
  void assignsFieldOfExistingObject(ParenthesizedAssignment p) {
    // :: error: (purity.incorrect.sideeffectsonly)
    (p).g = 1;
    // :: error: (purity.incorrect.sideeffectsonly)
    (p.g) = 2;
    // :: error: (purity.incorrect.sideeffectsonly)
    (this.other).f = 3;
  }

  @SideEffectsOnly("this.f")
  void assignsFieldOfReassignedLocal(ParenthesizedAssignment p) {
    ParenthesizedAssignment fresh = new ParenthesizedAssignment();
    (fresh) = p;
    // :: error: (purity.incorrect.sideeffectsonly)
    (fresh).g = 1;
  }
}
