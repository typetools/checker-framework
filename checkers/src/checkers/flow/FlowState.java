package checkers.flow;

import java.util.Set;

import checkers.types.QualifierHierarchy;
import javax.lang.model.element.AnnotationMirror;

/**
 * The state that is managed by a flow inference implementation.
 */
public interface FlowState {
    /**
     * Create a new instance of the current flow state class.
     * The type of the returned object has to be the same as the type of the receiver.
     * This is used in {@link #copy()} to create a new instance of the correct type.
     *
     * @param annotations The annotations that can be inferred.
     * @return A new flow state instance.
     */
    FlowState createFlowState(Set<AnnotationMirror> annotations);

    /**
     * The current set of annotations that can be inferred.
     *
     * @return A reference to the set of annotations.
     */
    // TODO: for annotations with values, we will need to modify this.
    Set<AnnotationMirror> getAnnotations();

    /**
     * Deeply copy the state of the current flow state.
     * The type of the returned object has to be the same as the type of the receiver.
     *
     * @return A completely independent copy of this.
     */
    FlowState copy();

    /**
     * "Or" the current state with an other state, modifying only the current state.
     *
     * @param other The other state, will not be modified.
     * @param annoRelations The relationship between the qualifiers.
     */
    void or(FlowState other, QualifierHierarchy annoRelations);

    /**
     * "And" the current state with an other state, modifying only the current state.
     *
     * @param other The other state, will not be modified.
     * @param annoRelations The relationship between the qualifiers.
     */
    void and(FlowState other, QualifierHierarchy annoRelations);
}