package org.checkerframework.dataflow.cfg.playground;

import org.checkerframework.dataflow.analysis.ForwardAnalysis;
import org.checkerframework.dataflow.analysis.ForwardAnalysisImpl;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;
import org.checkerframework.dataflow.constantpropagation.Constant;
import org.checkerframework.dataflow.constantpropagation.ConstantPropagationStore;
import org.checkerframework.dataflow.constantpropagation.ConstantPropagationTransfer;

/** Run constant propagation for a specific file and create a PDF of the CFG. */
public class ConstantPropagationPlayground {

  /** Do not instantiate. */
  private ConstantPropagationPlayground() {
    throw new Error("do not instantiate");
  }

  /**
   * Run constant propagation for a specific file and create a PDF of the CFG.
   *
   * @param args command-line arguments, not used
   */
  public static void main(String[] args) {

    /* Configuration: change as appropriate */
    String inputFile = "Test.java"; // input file name and path
    String outputDir = "cfg"; // output directory
    String method = "test"; // name of the method to analyze
    String clazz = "Test"; // name of the class to consider

    // run the analysis and create a PDF file
    ConstantPropagationTransfer transfer = new ConstantPropagationTransfer();
    ForwardAnalysis<Constant, ConstantPropagationStore, ConstantPropagationTransfer>
        forwardAnalysis = new ForwardAnalysisImpl<>(transfer);
    CFGVisualizeLauncher.generateDOTofCFG(
        inputFile, outputDir, method, clazz, true, true, forwardAnalysis);
  }
}
