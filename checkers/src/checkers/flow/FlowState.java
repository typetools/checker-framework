package checkers.flow;

import checkers.types.QualifierHierarchy;

public interface FlowState {
	FlowState copy();
	
	void or( FlowState other, QualifierHierarchy annoRelations);
	
	void and( FlowState other, QualifierHierarchy annoRelations);

}