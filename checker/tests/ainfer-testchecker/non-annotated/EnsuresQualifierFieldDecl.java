// This test ensures that a field having a non-default inferred type
// does not cause inference to issue an @EnsuresQualifier annotation
// stating that fact on every method in the class, even those in which
// the field is not mentioned (and therefore not in the store, making
// them unverifiable).

import org.checkerframework.checker.testchecker.ainfer.qual.Sibling1;

class EnsuresQualifierFieldDecl {
  @Sibling1 Object bar;

  // No annotation should be inferred here.
  void test() {}
}
