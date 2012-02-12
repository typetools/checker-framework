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
 * {@link CFAnalysis} is an extensible dataflow analysis for the Checker
 * Framework that tracks the annotations using a flow-sensitive analysis. It
 * uses an {@link AnnotatedTypeFactory} to provide checker-specific logic how to
 * combine types (e.g., what is the type of a string concatenation, given the
 * types of the two operands) and as an abstraction function (e.g., determine
 * the annotations on literals).
 * 
 * @author Charlie Garrett
 * @author Stefan Heule
 * 
 */
public class CFAnalysis extends Analysis<CFValue, CFStore, CFTransfer> {
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
