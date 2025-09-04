package org.checkerframework.dataflow.cfg.playground;

import org.checkerframework.dataflow.analysis.BackwardAnalysis;
import org.checkerframework.dataflow.analysis.BackwardAnalysisImpl;
import org.checkerframework.dataflow.analysis.UnusedAbstractValue;
import org.checkerframework.dataflow.busyexpr.BusyExprStore;
import org.checkerframework.dataflow.busyexpr.BusyExprTransfer;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeOptions;

/**
 * Run busy expression analysis on a file and create a PDF of the CFG.
 *
 * <p>As an example, try {@code dataflow/manual/examples/BusyExprSimple.java}.
 */
public class BusyExpressionPdf {

  /** Class cannot be instantiated. */
  private BusyExpressionPdf() {
    throw new Error("Do not instantiate");
  }

  /**
   * Run busy expression analysis on a file and create a PDF of the CFG.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {

    // Parse the arguments.
    CFGVisualizeOptions config = CFGVisualizeOptions.parseArgs(args);

    // Run the analysis and create a PDF file.
    BusyExprTransfer transfer = new BusyExprTransfer();
    BackwardAnalysis<UnusedAbstractValue, BusyExprStore, BusyExprTransfer> backwardAnalysis =
        new BackwardAnalysisImpl<>(transfer);
    CFGVisualizeLauncher.performAnalysis(config, backwardAnalysis);
  }
}
