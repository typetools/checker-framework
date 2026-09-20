package ajavatest;

// The ajava file for this class writes `@Unused(when = NestedAnno.class)` on field `f`.  The
// annotation file parser resolves the simple name `NestedAnno` relative to the class that it is
// currently parsing, so this test fails if the parser records that class under a name such as
// `ajavatest.ajavatest.AjavaClassLiteral`.

public class AjavaClassLiteral {

  /** An annotation type that is nested within the class being parsed. */
  public @interface NestedAnno {}

  Object f = new Object();

  Object getF() {
    return f;
  }
}
