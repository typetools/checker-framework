package livevar;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import org.checkerframework.dataflow.analysis.BackwardAnalysis;
import org.checkerframework.dataflow.analysis.BackwardAnalysisImpl;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;
import org.checkerframework.dataflow.livevariable.LiveVarStore;
import org.checkerframework.dataflow.livevariable.LiveVarTransfer;
import org.checkerframework.dataflow.livevariable.LiveVarValue;

/** Used in liveVariableTest Gradle task to test the LiveVariable analysis. */
public class LiveVariable {

  /**
   * The main method expects to be run in dataflow/tests/live-variable directory.
   *
   * @param args not used
   */
  public static void main(String[] args) {

    String inputFile = "Test.java";
    String method = "test";
    String clazz = "Test";
    String outputFile = "Out.txt";

    LiveVarTransfer transfer = new LiveVarTransfer();
    BackwardAnalysis<LiveVarValue, LiveVarStore, LiveVarTransfer> backwardAnalysis =
        new BackwardAnalysisImpl<>(transfer);
    CFGVisualizeLauncher cfgVisualizeLauncher = new CFGVisualizeLauncher();
    Map<String, Object> res =
        cfgVisualizeLauncher.generateStringOfCFG(inputFile, method, clazz, true, backwardAnalysis);
    try (FileWriter out = new FileWriter(outputFile)) {
      out.write(res.get("stringGraph").toString());
      out.write("\n");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
