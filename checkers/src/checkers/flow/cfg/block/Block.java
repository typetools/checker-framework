package checkers.flow.cfg.block;

import java.util.Map;


/**
 * Represents a basic block in a control flow graph.
 * 
 * @author Stefan Heule
 * 
 */
public interface Block {

	/**
	 * @return The unique identifier of this node.
	 */
	long getId();
	
	/**
	 * @return The list of exceptional successor blocks.
	 */
	Map<Class<? extends Throwable>, Block> getExceptionalSuccessors();

}
