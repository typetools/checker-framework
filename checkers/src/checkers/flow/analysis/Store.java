package checkers.flow.analysis;

public interface Store<A extends AbstractValue> {
	
	/** Create an exact copy of this store. */
	Store<A> copy();
}
