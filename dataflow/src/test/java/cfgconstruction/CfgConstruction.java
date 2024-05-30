package cfgconstruction;

import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.visualize.CfgVisualizeLauncher;

public class CfgConstruction {

  public static void main(String[] args) {

    String inputFile = "Test.java";
    String clazz = "Test";
    String method = "manyNestedTryFinallyBlocks";

    ControlFlowGraph cfg = CfgVisualizeLauncher.generateMethodCFG(inputFile, clazz, method);
    cfg.checkInvariants();
  }
}
