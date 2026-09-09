// Regression test for a bug in which WholeProgramInferenceScenesStorage recorded the declared
// type of a contract's expression under a key that did not identify the enclosing class.  Two
// same-signature methods in different classes that constrain the same expression collided, so one
// method's inferred precondition was compared against the other method's declared type, which
// suppressed a needed @RequiresQualifier annotation.
//
// All three classes are in one compilation unit, so that they are visited (and their annotation
// files written) in the order in which they appear below.

import org.checkerframework.checker.testchecker.ainfer.qual.AinferParent;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferTop;

class SameSignatureContracts {

  @AinferTop int field;

  @AinferParent int parent;

  // WPI should infer @RequiresQualifier(expression="this.field", qualifier=AinferParent.class).
  void m() {
    // :: warning: [assignment]
    @AinferParent int x = field;
  }

  void other() {}

  void callM() {
    field = parent;
    m();
  }
}

// This class's m() has the same JVM signature as SameSignatureContracts.m(), and its "field" has
// the same name as SameSignatureContracts.field but a different declared type.  No precondition is
// inferred for this m(), because the declared type of its "field" already implies the inferred
// type.  That must not suppress the precondition inferred for SameSignatureContracts.m().
class SameSignatureContracts2 {

  @AinferParent int field;

  void m() {
    @AinferParent int y = field;
  }

  void callM() {
    m();
  }
}

// Calling a method of SameSignatureContracts marks its annotation file as modified, so that the
// file is written again -- this time after SameSignatureContracts2 has been processed.
class SameSignatureContracts3 {

  void callOther(SameSignatureContracts a) {
    a.other();
  }
}
