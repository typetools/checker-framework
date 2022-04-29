package reachingdef;

import org.checkerframework.dataflow.analysis.ForwardAnalysis;
import org.checkerframework.dataflow.analysis.ForwardAnalysisImpl;
import org.checkerframework.dataflow.analysis.UnusedAbstractValue;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;
import org.checkerframework.dataflow.reachingdef.ReachingDefinitionStore;
import org.checkerframework.dataflow.reachingdef.ReachingDefinitionTransfer;

/** Used in reachingDefinitionsTest Gradle task to test the ReachingDefinition analysis. */
public class ReachingDefinition {

    /**
     * The main method expects to be run in dataflow/tests/reaching-definitions directory.
     *
     * @param args not used
     */
    public static void main(String[] args) {

        String inputFile = "Test.java";
        String method = "test";
        String clas = "Test";
        String outputFile = "Out.txt";

        ReachingDefinitionTransfer transfer = new ReachingDefinitionTransfer();
        ForwardAnalysis<UnusedAbstractValue, ReachingDefinitionStore, ReachingDefinitionTransfer>
                forwardAnalysis = new ForwardAnalysisImpl<>(transfer);
        CFGVisualizeLauncher.writeStringOfCFG(inputFile, method, clas, outputFile, forwardAnalysis);
    }
}
