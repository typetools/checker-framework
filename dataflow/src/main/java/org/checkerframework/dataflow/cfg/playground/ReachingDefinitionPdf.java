package org.checkerframework.dataflow.cfg.playground;

import org.checkerframework.dataflow.analysis.ForwardAnalysis;
import org.checkerframework.dataflow.analysis.ForwardAnalysisImpl;
import org.checkerframework.dataflow.analysis.UnusedAbstractValue;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeOptions;
import org.checkerframework.dataflow.reachingdef.ReachingDefinitionStore;
import org.checkerframework.dataflow.reachingdef.ReachingDefinitionTransfer;

/**
 * Run reaching definition analysis on a file and create a PDF of the CFG.
 *
 * <p>As an example, try {@code dataflow/manual/examples/ReachSimple.java}.
 */
public class ReachingDefinitionPdf {

  /** Class cannot be instantiated. */
  private ReachingDefinitionPdf() {
    throw new Error("Do not instantiate");
  }

  /**
   * Run reaching definition analysis on a file and create a PDF of the CFG.
   *
   * @param args input arguments
   */
  public static void main(String[] args) {

    // Parse the arguments.
    CFGVisualizeOptions config = CFGVisualizeOptions.parseArgs(args);

    // Run the analysis and create a PDF file.
    ReachingDefinitionTransfer transfer = new ReachingDefinitionTransfer();
    ForwardAnalysis<UnusedAbstractValue, ReachingDefinitionStore, ReachingDefinitionTransfer>
        forwardAnalysis = new ForwardAnalysisImpl<>(transfer);
    CFGVisualizeLauncher.performAnalysis(config, forwardAnalysis);
  }
}
