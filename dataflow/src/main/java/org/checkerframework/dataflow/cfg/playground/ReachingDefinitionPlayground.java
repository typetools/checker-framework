package org.checkerframework.dataflow.cfg.playground;

import org.checkerframework.dataflow.analysis.ForwardAnalysis;
import org.checkerframework.dataflow.analysis.ForwardAnalysisImpl;
import org.checkerframework.dataflow.analysis.UnusedAbstractValue;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;
import org.checkerframework.dataflow.reachingdef.ReachingDefinitionStore;
import org.checkerframework.dataflow.reachingdef.ReachingDefinitionTransfer;

/** The playground of reaching definition analysis. */
public class ReachingDefinitionPlayground {
  /**
   * Run reaching definition analysis for a specific file and create a PDF of the CFG in the end.
   *
   * @param args input arguments, not used
   */
  public static void main(String[] args) {

    /* Configuration: change as appropriate */
    String inputFile = "./dataflow/manual/examples/ReachSimple.java"; // input file name and path
    String outputDir = "."; // output directory
    String method = "test"; // name of the method to analyze
    String clazz = "Test"; // name of the class to consider

    // Run the analysis and create a PDF file
    ReachingDefinitionTransfer transfer = new ReachingDefinitionTransfer();
    ForwardAnalysis<UnusedAbstractValue, ReachingDefinitionStore, ReachingDefinitionTransfer>
        forwardAnalysis = new ForwardAnalysisImpl<>(transfer);
    CFGVisualizeLauncher.generateDOTofCFG(
        inputFile, outputDir, method, clazz, true, true, forwardAnalysis);
  }
}
