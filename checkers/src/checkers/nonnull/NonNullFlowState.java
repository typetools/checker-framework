package checkers.nonnull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

import checkers.flow.DefaultFlowState;
import checkers.flow.FlowState;
import checkers.types.QualifierHierarchy;

import com.sun.source.tree.VariableTree;

/**
 * The state needed for NullnessFlow.
 * 
 * @see DefaultFlowState
 * @see NullnessFlow
 */
//Note: this code is originally based on NullnessFlowState
public class NonNullFlowState extends DefaultFlowState {
	/**
	 * A list of non-null expressions. These are exact String representations of
	 * the corresponding Tree instances.
	 */
	List<String> nnExprs;
	List<VariableTree> varTrees = new LinkedList<>();

	NonNullFlowState(Set<AnnotationMirror> annotations) {
		super(annotations);
		nnExprs = new ArrayList<String>();
	}

	@Override
	public NonNullFlowState createFlowState(Set<AnnotationMirror> annotations) {
		return new NonNullFlowState(annotations);
	}

	@Override
	public NonNullFlowState copy() {
		NonNullFlowState res = (NonNullFlowState) super.copy();
		res.nnExprs = new ArrayList<String>(this.nnExprs);
		res.varTrees = new LinkedList<>(this.varTrees);
		// FLOWTODO: Copy initializedFields
		return res;
	}

	@Override
	public void or(FlowState other, QualifierHierarchy annoRelations) {
		NonNullFlowState nfs = (NonNullFlowState) other;
		super.or(other, annoRelations);
		addExtras(nnExprs, nfs.nnExprs);
	}

	private static <T> void addExtras(List<T> mod, List<T> add) {
		for (T a : add) {
			if (!mod.contains(a)) {
				mod.add(a);
			}
		}
	}

	@Override
	public void and(FlowState other, QualifierHierarchy annoRelations) {
		NonNullFlowState nfs = (NonNullFlowState) other;
		super.and(other, annoRelations);
		nnExprs.retainAll(nfs.nnExprs);
	}

	@Override
	public String toString() {
		return super.toString() + "\n" + "  nnExprs: " + nnExprs;
	}
}
