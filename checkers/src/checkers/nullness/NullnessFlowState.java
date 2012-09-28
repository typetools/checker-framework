package checkers.nullness;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

import checkers.flow.DefaultFlowState;
import checkers.flow.FlowState;
import checkers.types.QualifierHierarchy;

/**
 * The state needed for NullnessFlow.
 *
 * @see DefaultFlowState
 * @see NullnessFlow
 */
public class NullnessFlowState extends DefaultFlowState {
    /**
     * A list of non-null expressions.
     * These are exact String representations of the corresponding Tree instances.
     */
    List<String> nnExprs;

    NullnessFlowState(Set<AnnotationMirror> annotations) {
        super(annotations);
        nnExprs = new ArrayList<String>();
    }

    @Override
    public NullnessFlowState createFlowState(Set<AnnotationMirror> annotations) {
        return new NullnessFlowState(annotations);
    }

    @Override
    public NullnessFlowState copy() {
        NullnessFlowState res = (NullnessFlowState) super.copy();
        res.nnExprs = new ArrayList<String>(this.nnExprs);
        // TODO: Copy initializedFields
        return res;
    }

    @Override
    public void or(FlowState other, QualifierHierarchy annoRelations) {
        NullnessFlowState nfs = (NullnessFlowState) other;
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
        NullnessFlowState nfs = (NullnessFlowState) other;
        super.and(other, annoRelations);
        nnExprs.retainAll(nfs.nnExprs);
    }

    @Override
    public String toString() {
        return super.toString() + "\n" +
        "  nnExprs: " + nnExprs;
    }
}
