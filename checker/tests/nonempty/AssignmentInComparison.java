import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nonempty.qual.NonEmpty;

// The Non-Empty Checker unrefines aliases at a side effect.  A call in the right-hand side of an
// assignment does not change the value that the assignment stores, so comparing the assignment
// refines its target.
class AssignmentInComparison {

  List<String> list = new ArrayList<>();

  // Not side-effect-free.
  List<String> newList() {
    return new ArrayList<>();
  }

  static void callInRhs(AssignmentInComparison h, @NonEmpty List<String> ne) {
    if ((h.list = h.newList()) == ne) {
      @NonEmpty List<String> l = h.list;
    }
  }
}
