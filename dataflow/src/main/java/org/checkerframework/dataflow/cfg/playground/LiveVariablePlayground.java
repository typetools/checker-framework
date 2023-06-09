package org.checkerframework.dataflow.cfg.playground;

import org.checkerframework.dataflow.analysis.BackwardAnalysis;
import org.checkerframework.dataflow.analysis.BackwardAnalysisImpl;
import org.checkerframework.dataflow.analysis.UnusedAbstractValue;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeOptions;
import org.checkerframework.dataflow.livevariable.LiveVarStore;
import org.checkerframework.dataflow.livevariable.LiveVarTransfer;

/** The playground of live variable analysis. */
public class LiveVariablePlayground {

  /** Do not instantiate. */
  private LiveVariablePlayground() {
    throw new Error("do not instantiate");
  }

  /**
   * Run live variable analysis on a file.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {

    // Parse the arguments.
    CFGVisualizeOptions config = CFGVisualizeOptions.parseArgs(args);

    // Run the analysis and create a PDF file
    LiveVarTransfer transfer = new LiveVarTransfer();
    BackwardAnalysis<UnusedAbstractValue, LiveVarStore, LiveVarTransfer> backwardAnalysis =
        new BackwardAnalysisImpl<>(transfer);
    CFGVisualizeLauncher.performAnalysis(config, backwardAnalysis);
  }
}
