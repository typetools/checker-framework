import java.util.Map;
import org.checkerframework.dataflow.analysis.BackwardAnalysis;
import org.checkerframework.dataflow.analysis.BackwardAnalysisImpl;
import org.checkerframework.dataflow.cfg.CFGVisualizeLauncher;
import org.checkerframework.dataflow.livevariable.LiveVar;
import org.checkerframework.dataflow.livevariable.LiveVarStore;
import org.checkerframework.dataflow.livevariable.LiveVarTransfer;

public class LiveVariable {

    public static void main(String[] args) {

        String inputFile = "Test.java";
        String method = "test";
        String clazz = "Test";

        LiveVarTransfer transfer = new LiveVarTransfer();
        BackwardAnalysis<LiveVar, LiveVarStore, LiveVarTransfer> backwardAnalysis =
                new BackwardAnalysisImpl<>(transfer);
        CFGVisualizeLauncher cfgVisualizeLauncher = new CFGVisualizeLauncher();
        Map<String, Object> res =
                cfgVisualizeLauncher.generateStringOfCFG(
                        inputFile, method, clazz, true, backwardAnalysis);
        System.out.println(res.get("stringGraph"));
    }
}
