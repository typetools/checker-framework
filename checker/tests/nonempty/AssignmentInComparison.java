import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nonempty.qual.NonEmpty;

// The Non-Empty Checker unrefines aliases at a side effect.  A call in the right-hand side of an
// assignment does not change the value that the assignment stores, so comparing the assignment
// refines its target.
class AssignmentInComparison {

  // Not side-effect-free.
  List<String> newList() {
    return new ArrayList<>();
  }

  // Not side-effect-free.
  @NonEmpty List<String> makeNonEmpty() {
    List<String> result = new ArrayList<>();
    result.add("x");
    return result;
  }

  static class Holder {
    List<String> list = new ArrayList<>();
  }

  // The call might change the abstract value of the object that `h` denotes, but `h` still denotes
  // the same object, so the assignment refines `h.list`.
  void callInRhsOfFieldAssignment(Holder h) {
    h.list = makeNonEmpty();
    @NonEmpty List<String> m = h.list;
  }

  void callInRhs(@NonEmpty List<String> ne) {
    List<String> l;
    if ((l = newList()) == ne) {
      @NonEmpty List<String> m = l;
    }
  }
}
