package org.checkerframework.dataflow.cfg.playground;

import org.checkerframework.dataflow.analysis.BackwardAnalysis;
import org.checkerframework.dataflow.analysis.BackwardAnalysisImpl;
import org.checkerframework.dataflow.analysis.UnusedAbstractValue;
import org.checkerframework.dataflow.busyexpr.BusyExprStore;
import org.checkerframework.dataflow.busyexpr.BusyExprTransfer;
import org.checkerframework.dataflow.cfg.visualize.CfgVisualizeLauncher;
import org.checkerframework.dataflow.cfg.visualize.CfgVisualizeOptions;

/**
 * The playground for busy expression analysis. As an example, try {@code
 * dataflow/manual/examples/BusyExprSimple.java}.
 */
public class BusyExpressionPlayground {

  /** Class cannot be instantiated. */
  private BusyExpressionPlayground() {
    throw new AssertionError("Class BusyExpressionPlayground cannot be instantiated.");
  }

  /**
   * Run busy expression analysis on a file.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {

    // Parse the arguments.
    CfgVisualizeOptions config = CfgVisualizeOptions.parseArgs(args);

    // Run the analysis and create a PDF file
    BusyExprTransfer transfer = new BusyExprTransfer();
    BackwardAnalysis<UnusedAbstractValue, BusyExprStore, BusyExprTransfer> backwardAnalysis =
        new BackwardAnalysisImpl<>(transfer);
    CfgVisualizeLauncher.performAnalysis(config, backwardAnalysis);
  }
}
