package checkers.flow.analysis.checkers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

import checkers.flow.analysis.Analysis;
import checkers.flow.cfg.CFGDOTVisualizer;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.QualifierHierarchy;


/**
 * DefaultTypeAnalysis characterizes a kind of abstract value that is computed
 * by the checker framework's default flow-sensitive analysis. It is
 * parameterized by a QualifierHierarchy, defining the relationship among type
 * annotations and an AnnotatedTypeFactory, providing the static type
 * annotations for AST Trees.
 * 
 * The inner class DefaultTypeAnalysis.Value represents a single abstract value
 * computed by the checker framework's default flow-sensitive analysis. A Value
 * is a set of type annotations from the QualifierHierarchy.
 * 
 * The inner class DefaultTypeAnalysis.NodeInfo represents the set of dataflow
 * facts known at a program point, which is a mapping from values, represented
 * by CFG Nodes, to sets of type annotations, represented by
 * DefaultTypeAnalysis.Values.
 * 
 * TODO: Since statically known annotations provided by the AnnotatedTypeFactory
 * are upper bounds, avoid storing NodeInfos explicitly unless they are more
 * precise than the static annotations.
 * 
 * The inner class DefaultTypeAnalysis.Transfer is the transfer function mapping
 * input dataflow facts to output facts. For the default analysis, it merely
 * tracks type annotations through assignments to local variables. Improvements
 * in the precision of type annotations arise from assignments whose RHS has a
 * more precise static type than their LHS.
 * 
 * @author Charlie Garrett
 * @author Stefan Heule
 * 
 */
public class CFAnalysis extends
		Analysis<CFValue, CFStore, CFTransfer> {
	/**
	 * The qualifier hierarchy for which to track annotations.
	 */
	protected final QualifierHierarchy typeHierarchy;

	/**
	 * A type factory that can provide static type annotations for AST Trees.
	 */
	protected final AnnotatedTypeFactory factory;

	/**
	 * The full set of annotations allowed for this type hierarchy.
	 */
	protected final Set<AnnotationMirror> legalAnnotations;

	public CFAnalysis(QualifierHierarchy typeHierarchy,
			AnnotatedTypeFactory factory) {
		super(new CFTransfer());
		this.typeHierarchy = typeHierarchy;
		this.legalAnnotations = typeHierarchy.getAnnotations();
		this.factory = factory;
		this.transferFunction.setAnalysis(this);
	}

	/**
	 * Print a DOT graph of the CFG and analysis info for inspection.
	 */
	public void outputToDotFile(String outputFile) {
		String s = CFGDOTVisualizer.visualize(cfg.getEntryBlock(), this);

		try {
			FileWriter fstream = new FileWriter(outputFile);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(s);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
