package checkers.flow.cfg.node;

import java.util.Collection;
import java.util.Collections;

import javax.lang.model.element.Element;

import checkers.flow.util.HashCodeUtils;
import checkers.util.TreeUtils;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

/**
 * A node for a local variable or a parameter:
 * 
 * <pre>
 *   <em>identifier</em>
 * </pre>
 * 
 * @author Stefan Heule
 * 
 */
// TODO: don't use for parameters, as they don't have a tree
public class LocalVariableNode extends Node {

	protected Tree tree;

	// Declaration of this variable, or null if not found during CFG
	// construction.
	protected VariableDeclarationNode decl;

	public LocalVariableNode(Tree t, VariableDeclarationNode d) {
		// IdentifierTree for normal uses of the local variable or parameter,
		// and VariableTree for the translation of an initilizer block
		assert t != null;
		assert t instanceof IdentifierTree || t instanceof VariableTree;
		tree = t;
		decl = d;
	}

	public Element getElement() {
		Element el;
		if (tree instanceof IdentifierTree) {
			el = TreeUtils.elementFromUse((IdentifierTree) tree);
		} else {
			assert tree instanceof VariableTree;
			el = TreeUtils.elementFromDeclaration((VariableTree) tree);
		}
		return el;
	}

	public String getName() {
		if (tree instanceof IdentifierTree) {
			return ((IdentifierTree) tree).getName().toString();
		}
		return ((VariableTree) tree).getName().toString();
	}

	@Override
	public Tree getTree() {
		return tree;
	}

	public VariableDeclarationNode getDeclaration() {
		return decl;
	}

	@Override
	public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
		return visitor.visitLocalVariable(this, p);
	}

	@Override
	public String toString() {
		return getName().toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof LocalVariableNode)) {
			return false;
		}
		LocalVariableNode other = (LocalVariableNode) obj;
		return getName().equals(other.getName());
	}

	@Override
	public int hashCode() {
		return HashCodeUtils.hash(getName());
	}

	@Override
	public Collection<Node> getOperands() {
		return Collections.emptyList();
	}

	@Override
	public boolean hasResult() {
		return true;
	}
}
