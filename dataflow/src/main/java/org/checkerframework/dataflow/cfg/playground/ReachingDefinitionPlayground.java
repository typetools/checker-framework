package org.checkerframework.dataflow.cfg.playground;

import org.checkerframework.dataflow.analysis.ForwardAnalysis;
import org.checkerframework.dataflow.analysis.ForwardAnalysisImpl;
import org.checkerframework.dataflow.analysis.UnusedAbstractValue;
import org.checkerframework.dataflow.cfg.visualize.CfgVisualizeLauncher;
import org.checkerframework.dataflow.cfg.visualize.CfgVisualizeOptions;
import org.checkerframework.dataflow.reachingdef.ReachingDefinitionStore;
import org.checkerframework.dataflow.reachingdef.ReachingDefinitionTransfer;

/**
 * The playground for reaching definition analysis. As an example, try {@code
 * dataflow/manual/examples/ReachSimple.java}.
 */
public class ReachingDefinitionPlayground {

  /** Class cannot be instantiated. */
  private ReachingDefinitionPlayground() {
    throw new AssertionError("Class ReachingDefinitionPlayground cannot be instantiated.");
  }

  /**
   * Run reaching definition analysis for a specific file and create a PDF of the CFG in the end.
   *
   * @param args input arguments, not used
   */
  public static void main(String[] args) {

    // Parse the arguments.
    CfgVisualizeOptions config = CfgVisualizeOptions.parseArgs(args);

    // Run the analysis and create a PDF file
    ReachingDefinitionTransfer transfer = new ReachingDefinitionTransfer();
    ForwardAnalysis<UnusedAbstractValue, ReachingDefinitionStore, ReachingDefinitionTransfer>
        forwardAnalysis = new ForwardAnalysisImpl<>(transfer);
    CfgVisualizeLauncher.performAnalysis(config, forwardAnalysis);
  }
}
