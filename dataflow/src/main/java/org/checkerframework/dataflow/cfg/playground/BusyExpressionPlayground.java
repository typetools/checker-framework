package org.checkerframework.dataflow.cfg.playground;

import org.checkerframework.dataflow.analysis.BackwardAnalysis;
import org.checkerframework.dataflow.analysis.BackwardAnalysisImpl;
import org.checkerframework.dataflow.analysis.UnusedAbstractValue;
import org.checkerframework.dataflow.busyexpr.BusyExprStore;
import org.checkerframework.dataflow.busyexpr.BusyExprTransfer;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;

/** The playground for busy expression analysis */
public class BusyExpressionPlayground {

  /**
   * Run busy expression analysis playground on a test file and print the CFG graph
   *
   * @param args input arguments
   */
  public static void main(String[] args) {

    /* Configuration: change as appropriate */
    String inputFile = "dataflow/manual/examples/BusyExprSimple.java"; // input file name and path
    String outputDir = "."; // output directory
    String method = "test"; // name of the method to analyze
    String clazz = "Test"; // name of the class to consider

    // Run the analysis and create a PDF file
    BusyExprTransfer transfer = new BusyExprTransfer();
    BackwardAnalysis<UnusedAbstractValue, BusyExprStore, BusyExprTransfer> backwardAnalysis =
        new BackwardAnalysisImpl<>(transfer);
    CFGVisualizeLauncher.generateDOTofCFG(
        inputFile, outputDir, method, clazz, true, true, backwardAnalysis);
  }
}
