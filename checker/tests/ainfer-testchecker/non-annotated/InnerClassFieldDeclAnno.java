// Tests that ajava-based inference places declaration annotations on
// inner class fields in the correct place. Declaration annotations (unlike
// type annotations) need to be placed before the name of the outer class.
// E.g., "@DeclAnno Outer.Inner field;" rather than "Outer.@DeclAnno Inner field;".

import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferTreatAsSibling1;

public class InnerClassFieldDeclAnno {
  static class Outer {
    static class Inner {}
  }

  public Outer.Inner iShouldBeTreatedAsSibling1 = new Outer.Inner();

  @AinferTreatAsSibling1 public Outer.Inner preAnnotated = null;

  public static void test(InnerClassFieldDeclAnno a) {
    // :: warning: (assignment)
    @AinferSibling1 Object obj = a.iShouldBeTreatedAsSibling1;
    // Test that the annotation works as expected.
    @AinferSibling1 Object obj2 = a.preAnnotated;
  }
}
