package implicitthis;

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.type.DeclaredType;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.node.ImplicitThisNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;

/**
 * Used in the implicitThisTest Gradle task to test the types of implicit receivers ({@link
 * ImplicitThisNode}s) in the CFG.
 */
public class ImplicitThis {

  /** Do not instantiate. */
  private ImplicitThis() {
    throw new Error("Do not instantiate");
  }

  /**
   * The main method expects to be run in the dataflow/tests/implicit-this directory.
   *
   * @param args not used
   */
  public static void main(String[] args) {
    List<String> failures = new ArrayList<>();
    // Each row is: method, expected simple name of the type of every implicit receiver in that
    // method.  Method names are unique within Test.java.
    String[][] expectations = {
      {"inSub", "Issue5174Sub"},
      {"inNestedOuterMembers", "Issue5174Sub"},
      {"inNestedOwnMembers", "SubNested"},
    };
    for (String[] expectation : expectations) {
      String method = expectation[0];
      String expected = expectation[1];
      // CFGVisualizeLauncher requires a top-level class, even for a method of a nested class.
      ControlFlowGraph cfg =
          CFGVisualizeLauncher.generateMethodCFG(
              "Test.java", method, "Issue5174Sub", /* analysis= */ null);
      int numImplicitThis = 0;
      for (Node node : cfg.getAllNodes()) {
        if (node instanceof ImplicitThisNode) {
          numImplicitThis++;
          String actual = ((DeclaredType) node.getType()).asElement().getSimpleName().toString();
          if (!actual.equals(expected)) {
            failures.add(
                String.format(
                    "%s: implicit receiver has type %s, expected %s",
                    method, node.getType(), expected));
          }
        }
      }
      if (numImplicitThis == 0) {
        failures.add(String.format("%s: no implicit receiver found", method));
      }
    }
    if (!failures.isEmpty()) {
      throw new Error(String.join(System.lineSeparator(), failures));
    }
  }
}
