package checkers.flow.cfg.node;


import checkers.flow.util.HashCodeUtils;

import com.sun.source.tree.Tree;
import com.sun.source.tree.LiteralTree;

/**
 * A node for a literals that have some form of value:
 * <ul>
 * <li>integer literal</li>
 * <li>long literal</li>
 * <li>char literal</li>
 * <li>string literal</li>
 * <li>float literal</li>
 * <li>double literal</li>
 * <li>boolean literal</li>
 * <li>null literal</li>
 * </ul>
 * 
 * @author Stefan Heule
 * 
 */
public abstract class ValueLiteralNode extends Node {
	
	protected LiteralTree tree;
	
	/**
	 * @return The literal tree.
	 */
	public LiteralTree getLiteralTree() {
		return tree;
	}
	
	/**
	 * @return The value of the literal.
	 */
	abstract public Object getValue();

	@Override
	public Tree getTree() {
		return getLiteralTree();
	}
	
	@Override
	public String toString() {
		return getValue().toString();
	}
	
	/**
	 * Compare the value of this nodes.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof ValueLiteralNode)) {
			return false;
		}
		ValueLiteralNode other = (ValueLiteralNode) obj;
		return getValue().equals(other.getValue());
	}
	
	@Override
	public int hashCode() {
		return HashCodeUtils.hash(getValue());
	}

}
