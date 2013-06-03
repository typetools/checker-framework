package dataflow.cfg.playground;

import dataflow.analysis.Analysis;
import dataflow.cfg.JavaSource2CFGDOT;
import dataflow.constantpropagation.Constant;
import dataflow.constantpropagation.ConstantPropagationStore;
import dataflow.constantpropagation.ConstantPropagationTransfer;

public class ConstantPropagationPlayground {

    /**
     * Run constant propagation for a specific file and create a PDF of the CFG
     * in the end.
     */
    public static void main(String[] args) {

        /* Configuration: change as appropriate */
        String inputFile = "cfg-input.java"; // input file name and path
        String outputFileName = "cfg"; // output file name and path (without
                                       // extension)
        String method = "test"; // name of the method to analyze
        String clazz = "Test"; // name of the class to consider

        // run the analysis and create a PDF file
        ConstantPropagationTransfer transfer = new ConstantPropagationTransfer();
        // TODO: correct processing environment
        Analysis<Constant, ConstantPropagationStore, ConstantPropagationTransfer> analysis = new Analysis<>(
                null, transfer);
        JavaSource2CFGDOT.generateDOTofCFG(inputFile, outputFileName, method,
                clazz, true, analysis);
    }

}
