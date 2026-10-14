import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

// A binding variable declared by a type pattern in a case label is refined by the qualifiers of
// the selector expression, as one declared by an instanceof pattern is.
public class CaseTypePatternRefinement {

  void untainted(@Untainted Object o) {
    switch (o) {
      case String s -> {
        @Untainted String t = s;
      }
      default -> {}
    }
  }

  void tainted(@Tainted Object o) {
    switch (o) {
      case String s -> {
        // :: error: (assignment)
        @Untainted String t = s;
      }
      default -> {}
    }
  }

  <T extends @Untainted Object> void typeVariable(T o) {
    switch (o) {
      case String s -> {
        @Untainted String t = s;
      }
      default -> {}
    }
  }

  void statementGroup(@Untainted Object o) {
    switch (o) {
      case Integer i:
        break;
      case String s:
        @Untainted String t = s;
        break;
      default:
        break;
    }
  }

  @Untainted String switchExpression(@Untainted Object o) {
    return switch (o) {
      case String s -> s;
      default -> "";
    };
  }
}
