// A local variable that is assigned only expressions that the `@SideEffectsOnly` annotation covers
// is an alias of them, so modifying the variable's value is covered too.

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class AliasedLocalSeonly {

  List<Integer> covered = new ArrayList<>();

  static List<Integer> uncovered = new ArrayList<>();

  @SideEffectsOnly("this")
  void aliasOfField() {
    List<Integer> alias = this.covered;
    alias.add(1);
  }

  @SideEffectsOnly("#1")
  void aliasOfParameter(List<Integer> lst) {
    List<Integer> alias;
    alias = lst;
    alias.add(1);
  }

  @SideEffectsOnly("this")
  void aliasOfAlias() {
    List<Integer> alias1 = covered;
    List<Integer> alias2 = alias1;
    alias2.add(1);
  }

  @SideEffectsOnly("this")
  void aliasOfUncovered() {
    List<Integer> alias = uncovered;
    // :: error: (purity.incorrect.sideeffectsonly)
    alias.add(1);
  }

  @SideEffectsOnly("this")
  void aliasReassigned(boolean b) {
    List<Integer> alias = covered;
    if (b) {
      alias = uncovered;
    }
    // :: error: (purity.incorrect.sideeffectsonly)
    alias.add(1);
  }

  @SideEffectsOnly("this")
  void aliasInLoop() {
    // An enhanced `for` loop's variable is not an alias of any expression.
    for (List<Integer> alias : List.of(covered)) {
      // :: error: (purity.incorrect.sideeffectsonly)
      alias.add(1);
    }
  }
}
