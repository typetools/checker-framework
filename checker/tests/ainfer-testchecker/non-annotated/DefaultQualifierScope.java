import org.checkerframework.checker.testchecker.ainfer.qual.AinferDefaultType;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferTop;
import org.checkerframework.framework.qual.DefaultQualifier;

// `@AinferDefaultType` is the default qualifier of the hierarchy, but in the scope of
// `@DefaultQualifier(AinferTop.class)`, the qualifier of an unannotated type is `@AinferTop`.
// Whole-program inference infers `@AinferDefaultType` for the formal parameter of `take`.  It must
// write that annotation, because omitting it would leave the formal parameter `@AinferTop`.
@SuppressWarnings("annotation.not.allowed.in.src") // `@AinferDefaultType` is written below.
public class DefaultQualifierScope {

  @DefaultQualifier(AinferTop.class)
  static class InScope {
    static void take(Object o) {
      // :: warning: [argument]
      expectsDefaultType(o);
    }
  }

  static void expectsDefaultType(@AinferDefaultType Object o) {}

  static void client(Object o) {
    InScope.take(o);
  }
}
