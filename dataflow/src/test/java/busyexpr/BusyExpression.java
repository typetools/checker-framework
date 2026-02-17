package busyexpr;

import org.checkerframework.dataflow.analysis.BackwardAnalysis;
import org.checkerframework.dataflow.analysis.BackwardAnalysisImpl;
import org.checkerframework.dataflow.analysis.UnusedAbstractValue;
import org.checkerframework.dataflow.busyexpr.BusyExprStore;
import org.checkerframework.dataflow.busyexpr.BusyExprTransfer;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;

/**
 * Run busy expression analysis create a text file of the CFG.
 *
 * <p>Used in busyExpressionTest Gradle task to test the BusyExpression analysis.
 */
public class BusyExpression {
  /**
   * The main method expects to be run in the {@code dataflow/tests/busy-expression/} directory.
   *
   * @param args command-line arguments, not used
   */
  public static void main(String[] args) {

    String inputFile = "Test.java";
    String method = "test";
    String clazz = "Test";
    String outputFile = "Out.txt";

    BusyExprTransfer transfer = new BusyExprTransfer();
    BackwardAnalysis<UnusedAbstractValue, BusyExprStore, BusyExprTransfer> backwardAnalysis =
        new BackwardAnalysisImpl<>(transfer);
    ControlFlowGraph cfg =
        CFGVisualizeLauncher.generateMethodCFG(inputFile, method, clazz, backwardAnalysis);
    CFGVisualizeLauncher.writeStringOfCFG(cfg, outputFile, backwardAnalysis);
    // The .dot and .pdf files are not tested, only created for debugging convenience.
    CFGVisualizeLauncher.generateDOTofCFG(cfg, ".", true, true, backwardAnalysis);
  }
}
