import org.checkerframework.checker.testchecker.ainfer.qual.AinferDefaultType;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferTop;
import org.checkerframework.framework.qual.DefaultQualifier;

// `@AinferDefaultType` is the default qualifier of the hierarchy, but in the scope of
// `@DefaultQualifier(AinferTop.class)`, the qualifier of an unannotated type is `@AinferTop`.
// Whole-program inference infers `@AinferDefaultType` for the formal parameter of `take`.  It must
// write that annotation, because omitting it would leave the formal parameter `@AinferTop`.
public class DefaultQualifierScope {

  @DefaultQualifier(AinferTop.class)
  static class InScope {
    @SuppressWarnings("annotation.not.allowed.in.src") // WPI writes `@AinferDefaultType` on `o`
    static void take(Object o) {
      // :: warning: [argument]
      expectsDefaultType(o);
    }
  }

  @SuppressWarnings("annotation.not.allowed.in.src") // this test needs `@AinferDefaultType` here
  static void expectsDefaultType(@AinferDefaultType Object o) {}

  static void client(Object o) {
    InScope.take(o);
  }
}
