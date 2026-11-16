import org.checkerframework.checker.testchecker.ainfer.qual.AinferTop;
import org.checkerframework.framework.qual.DefaultQualifier;

// `@AinferDefaultType` is the default qualifier of AinferRelevanceTestChecker.  Whole-program
// inference infers `@AinferDefaultType` for the field and for the formal parameter and return type
// of each method.  Writing `@AinferDefaultType` would be redundant, so the goal file omits it.
public class OmitDefaultQualifier {

  CharSequence field;

  void setField(CharSequence cs) {
    field = cs;
  }

  CharSequence getField() {
    return field;
  }

  // Like `InScopeOfDefaultQualifier`, but declared before the method that calls it.  Whole-program
  // inference processes this class, and writes this file, before it processes that method.
  @DefaultQualifier(AinferTop.class)
  static class DeclaredBeforeClient {
    static void take(CharSequence cs) {}
  }

  static void client(CharSequence cs) {
    OmitDefaultQualifier o = new OmitDefaultQualifier();
    o.setField(cs);
    o.getField();
    InScopeOfDefaultQualifier.take(cs);
    DeclaredBeforeClient.take(cs);
  }

  // In the scope of `@DefaultQualifier(AinferTop.class)`, the qualifier of an unannotated type is
  // `@AinferTop`, not `@AinferDefaultType`.  Inference infers `@AinferDefaultType` for the formal
  // parameter, and the goal file contains it, because omitting it would change the meaning of the
  // ajava file.
  @DefaultQualifier(AinferTop.class)
  static class InScopeOfDefaultQualifier {
    static void take(CharSequence cs) {}
  }
}
