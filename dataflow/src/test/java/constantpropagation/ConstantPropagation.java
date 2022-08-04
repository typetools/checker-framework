package constantpropagation;

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
        String clas = "Test";
        String outputFile = "Out.txt";

        ConstantPropagationTransfer transfer = new ConstantPropagationTransfer();
        ForwardAnalysis<Constant, ConstantPropagationStore, ConstantPropagationTransfer>
                forwardAnalysis = new ForwardAnalysisImpl<>(transfer);
        CFGVisualizeLauncher.writeStringOfCFG(inputFile, method, clas, outputFile, forwardAnalysis);
    }
}
