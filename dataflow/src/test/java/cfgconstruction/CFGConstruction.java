package cfgconstruction;

import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;

public class CFGConstruction {

  /** Do not instantiate. */
  private CFGConstruction() {
    throw new Error("Do not instantiate");
  }

  public static void main(String[] args) {

    String inputFile = "Test.java";
    String clazz = "Test";

    ControlFlowGraph cfg =
        CFGVisualizeLauncher.generateMethodCFG(
            inputFile, "manyNestedTryFinallyBlocks", clazz, /* analysis= */ null);
    cfg.checkInvariants();

    // The CFG builder translates the finally block several times, but the CFG lists each lambda
    // and class declaration in it once, so that each is analyzed once.
    cfg =
        CFGVisualizeLauncher.generateMethodCFG(
            inputFile, "lambdaAndClassInFinallyBlock", clazz, /* analysis= */ null);
    cfg.checkInvariants();
    if (cfg.getDeclaredLambdas().size() != 1) {
      throw new Error("Expected 1 declared lambda, found " + cfg.getDeclaredLambdas());
    }
    if (cfg.getDeclaredClasses().size() != 1) {
      throw new Error("Expected 1 declared class, found " + cfg.getDeclaredClasses());
    }
  }
}
