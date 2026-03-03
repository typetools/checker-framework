package org.checkerframework.dataflow.cfg.playground;

import org.checkerframework.dataflow.analysis.ForwardAnalysis;
import org.checkerframework.dataflow.analysis.ForwardAnalysisImpl;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeOptions;
import org.checkerframework.dataflow.constantpropagation.Constant;
import org.checkerframework.dataflow.constantpropagation.ConstantPropagationStore;
import org.checkerframework.dataflow.constantpropagation.ConstantPropagationTransfer;

/** Run constant propagation on a file and create a PDF of the CFG. */
public class ConstantPropagationPdf {

  /** Class cannot be instantiated. */
  private ConstantPropagationPdf() {
    throw new Error("Do not instantiate");
  }

  /**
   * Run constant propagation on a file and create a PDF of the CFG.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {

    // Parse the arguments.
    CFGVisualizeOptions config = CFGVisualizeOptions.parseArgs(args);

    // Run the analysis and create a PDF file.
    ConstantPropagationTransfer transfer = new ConstantPropagationTransfer();
    ForwardAnalysis<Constant, ConstantPropagationStore, ConstantPropagationTransfer>
        forwardAnalysis = new ForwardAnalysisImpl<>(transfer);
    CFGVisualizeLauncher.performAnalysis(config, forwardAnalysis);
  }
}
