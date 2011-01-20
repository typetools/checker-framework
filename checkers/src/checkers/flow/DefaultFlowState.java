package checkers.flow;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;

import checkers.flow.AbstractFlow.SplitTuple;
import checkers.types.QualifierHierarchy;


public class DefaultFlowState implements FlowState {
	protected Set<AnnotationMirror> annotations;
	
	public DefaultFlowState(Set<AnnotationMirror> annotations) {
		this.annotations = annotations;
		this.annos = new GenKillBits<AnnotationMirror>(annotations);
		// this.annosWhenTrue = null;
		// this.annosWhenFalse = null;
		this.vars = new ArrayList<VariableElement>();
	}

	public DefaultFlowState createFlowState(Set<AnnotationMirror> annotations) {
		return new DefaultFlowState(annotations);
	}
	
	
	/**
	 * Tracks the annotated state of each variable during flow. Bit indices
	 * correspond exactly to indices in {@link #vars}. This field is set to null
	 * immediately after splitting for a branch, and is set to some combination
	 * (usually boolean "and") of {@link SplitTuple}'s annosWhenTrue and
	 * annosWhenFalse after merging. Since it is used when visiting the true and
	 * false branches, however, it may be non-null concurrently with
	 * {@link SplitTuple}'s annosWhenTrue and annosWhenFalse.
	 */
	// public needed for an access in NullnessFlow :-(
	public GenKillBits<AnnotationMirror> annos;

	/**
	 * Tracks the annotated state of each variable in a true branch. As in
	 * {@code javac}'s {@code Flow}, saving/restoring via local variables
	 * handles nested branches. Bit indices correspond exactly to indices in
	 * {@link #vars}. This field is copied from {@link #annos} when splitting
	 * for a branch and is set to null immediately after merging.
	 * 
	 * @see #annos
	 */
	// protected GenKillBits<AnnotationMirror> annosWhenTrue;

	/**
	 * Maps variables to a bit index. This index is also used as the bit index
	 * to determine a variable's annotatedness using
	 * annos/annosWhenTrue/annosWhenFalse.
	 * 
	 * @see #annos
	 * @see SplitTuple
	 */
	// TODO: move to State??
	public List<VariableElement> vars;

	/**
	 * Tracks the annotated state of each variable in a false branch. As in
	 * {@code javac}'s {@code Flow}, saving/restoring via local variables
	 * handles nested branches. Bit indices correspond exactly to indices in
	 * {@link #vars}. This field is copied from {@link #annos} when splitting
	 * for a branch and is set to null immediately after merging.
	 * 
	 * @see #annos
	 */
	// protected GenKillBits<AnnotationMirror> annosWhenFalse;

	public DefaultFlowState copy() {
		DefaultFlowState res = createFlowState(this.annotations);
		res.annos = GenKillBits.copy(this.annos);
		res.vars = new ArrayList<VariableElement>(this.vars);
		return res;
	}

	@Override
	public void or(FlowState other, QualifierHierarchy annoRelations) {
		DefaultFlowState dfs = (DefaultFlowState) other;
		GenKillBits.orlub(this.annos, dfs.annos, annoRelations);
	}
	
	@Override
	public void and(FlowState other, QualifierHierarchy annoRelations) {
		DefaultFlowState dfs = (DefaultFlowState) other;
		GenKillBits.andlub(this.annos, dfs.annos, annoRelations);
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + ": vars: " + vars + "\n" +
		 	"  annos: " + annos;
	}
}
