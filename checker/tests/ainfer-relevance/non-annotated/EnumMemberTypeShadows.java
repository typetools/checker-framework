import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

// A member type of an anonymous class is unnameable, so inference resolves a name in the body of
// the enum below by searching the member types that the enum declares and inherits.  An enum
// inherits the member type `EnumDesc` from its implicit supertype `java.lang.Enum`, and that
// member type shadows the member class `EnumMemberTypeShadows.EnumDesc`.  The name "EnumDesc"
// below therefore refers to `java.lang.Enum.EnumDesc`, which is irrelevant, rather than to
// `EnumMemberTypeShadows.EnumDesc`, which is relevant because it is a subtype of `CharSequence`.
//
// Inference must discard the annotation on `field`, as the goal file shows.  If inference instead
// resolves the name to `EnumMemberTypeShadows.EnumDesc`, then it writes the annotation.
//
// Writing `@AinferSibling1` on a `java.lang.Enum.EnumDesc` is itself a relevance violation, which
// this test suppresses because the warning would appear in both passes of the test and the
// annotated copy of this file contains no expected diagnostics.
@SuppressWarnings("rawtypes")
public class EnumMemberTypeShadows {

  abstract static class EnumDesc implements CharSequence {}

  static Object anonymous =
      new Object() {

        enum Nested {
          CONSTANT;

          EnumDesc field;

          void assignField() {
            field = getSibling1();
          }

          void useField() {
            expectsSibling1(field);
          }

          @SuppressWarnings("anno.on.irrelevant")
          void expectsSibling1(@AinferSibling1 EnumDesc f) {}

          @SuppressWarnings("anno.on.irrelevant")
          @AinferSibling1 EnumDesc getSibling1() {
            return null;
          }
        }
      };
}
