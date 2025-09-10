package cfgconstruction;

import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;

public class CFGConstruction {

  public static void main(String[] args) {

    String inputFile = "Test.java";
    String clazz = "Test";
    String method = "manyNestedTryFinallyBlocks";

    ControlFlowGraph cfg =
        CFGVisualizeLauncher.generateMethodCFG(inputFile, method, clazz, /* analysis= */ null);
    cfg.checkInvariants();
  }
}
