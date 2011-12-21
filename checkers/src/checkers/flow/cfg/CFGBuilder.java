package checkers.flow.cfg;



import com.sun.source.tree.MethodTree;

/**
 * Builds the control flow graph of a Java method (represented by its abstract
 * syntax tree, {@link MethodTree}).
 * 
 * @author Stefan Heule
 * 
 */
public class CFGBuilder {

	/**
	 * Build the control flow graph of a method.
	 */
	public static BasicBlock build(MethodTree method) {
		return new CFGHelper().build(method.getBody());
	}
}
