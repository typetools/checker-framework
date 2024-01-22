package org.checkerframework.dataflow.cfg.playground;

import org.checkerframework.dataflow.analysis.ForwardAnalysis;
import org.checkerframework.dataflow.analysis.ForwardAnalysisImpl;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeOptions;
import org.checkerframework.dataflow.constantpropagation.Constant;
import org.checkerframework.dataflow.constantpropagation.ConstantPropagationStore;
import org.checkerframework.dataflow.constantpropagation.ConstantPropagationTransfer;

/** Run constant propagation for a specific file and create a PDF of the CFG. */
public class ConstantPropagationPlayground {

  /** Class cannot be instantiated. */
  private ConstantPropagationPlayground() {
    throw new AssertionError("Class ConstantPropagationPlayground cannot be instantiated.");
  }

  /**
   * Run constant propagation analysis on a file.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {

    // Parse the arguments.
    CFGVisualizeOptions config = CFGVisualizeOptions.parseArgs(args);

    // run the analysis and create a PDF file
    ConstantPropagationTransfer transfer = new ConstantPropagationTransfer();
    ForwardAnalysis<Constant, ConstantPropagationStore, ConstantPropagationTransfer>
        forwardAnalysis = new ForwardAnalysisImpl<>(transfer);
    CFGVisualizeLauncher.performAnalysis(config, forwardAnalysis);
  }
}
