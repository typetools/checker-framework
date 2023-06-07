package constantpropagation;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import org.checkerframework.dataflow.analysis.ForwardAnalysis;
import org.checkerframework.dataflow.analysis.ForwardAnalysisImpl;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;
import org.checkerframework.dataflow.constantpropagation.Constant;
import org.checkerframework.dataflow.constantpropagation.ConstantPropagationStore;
import org.checkerframework.dataflow.constantpropagation.ConstantPropagationTransfer;

public class ConstantPropagation {

  /**
   * The main method expects to be run in dataflow/tests/constant-propagation directory.
   *
   * @param args not used
   */
  public static void main(String[] args) {

    String inputFile = "Test.java";
    String method = "test";
    String clazz = "Test";
    String outputFile = "Out.txt";

    ConstantPropagationTransfer transfer = new ConstantPropagationTransfer();
    ForwardAnalysis<Constant, ConstantPropagationStore, ConstantPropagationTransfer>
        forwardAnalysis = new ForwardAnalysisImpl<>(transfer);
    Map<String, Object> res =
        CFGVisualizeLauncher.generateStringOfCFG(inputFile, method, clazz, true, forwardAnalysis);
    try (FileWriter out = new FileWriter(outputFile)) {
      out.write(res.get("stringGraph").toString());
      out.write("\n");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
