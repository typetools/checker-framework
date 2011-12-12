package checkers.flow;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;

import checkers.types.QualifierHierarchy;

/**
 * Stores the annotation that is inferred for each variable.
 */
public class DefaultFlowState implements FlowState {

    /**
     * The annotations (qualifiers) to infer. The relationship among them is
     * determined using the parameter to "and" and "or". By
     * consulting the hierarchy, the analysis will only infer a qualifier on a
     * type if it is more restrictive (i.e. a subtype) than the existing
     * qualifier for that type.
     */
    protected final Set<AnnotationMirror> annotations;

    /**
     * Maps variables to a bit index. This index is also used as the bit index
     * to determine a variable's annotatedness using annos.
     * In a previous implementation, vars was only copied when visitBlock or
     * visitMethod was invoked. We now copy this field whenever we need to copy the
     * state.
     * TODO: look whether it's worth to optimize this.
     *
     * @see #annos
     */
    public List<VariableElement> vars;

    /**
     * Tracks the annotated state of each variable during flow. Bit indices
     * correspond exactly to indices in {@link #vars}.
     */
    // public needed for an access in NullnessFlow :-(
    public GenKillBits<AnnotationMirror> annos;

    /**
     * Create a new default flow state.
     *
     * @param annotations The annotations that can be inferred.
     */
    public DefaultFlowState(Set<AnnotationMirror> annotations) {
        this.annotations = annotations;
        this.annos = new GenKillBits<AnnotationMirror>(annotations);
        this.vars = new ArrayList<VariableElement>();
    }

    @Override
    public DefaultFlowState createFlowState(Set<AnnotationMirror> annotations) {
        return new DefaultFlowState(annotations);
    }

    @Override
    public Set<AnnotationMirror> getAnnotations() {
        return annotations;
    }

    @Override
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
