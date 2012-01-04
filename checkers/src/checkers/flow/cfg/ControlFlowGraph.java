package checkers.flow.cfg;

import java.util.Map;

import checkers.flow.cfg.block.SpecialBlock;
import checkers.flow.cfg.node.Node;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;

/**
 * A control flow graph (CFG for short) of a single method.
 * 
 * @author Stefan Heule
 * 
 */
public class ControlFlowGraph {

	/** The entry block of the control flow graph. */
	protected SpecialBlock entryBlock;

	/** The method this CFG corresponds to. */
	protected MethodTree tree;

	/** Map from AST {@link Tree}s to {@link Node}s. */
	protected Map<Tree, Node> treeLookup;

	public ControlFlowGraph(SpecialBlock entryBlock, MethodTree tree,
			Map<Tree, Node> treeLookup) {
		super();
		this.entryBlock = entryBlock;
		this.tree = tree;
		this.treeLookup = treeLookup;
	}

	/**
	 * @return The {@link Node} to which the {@link Tree} <code>t</code>
	 *         corresponds.
	 */
	public Node getNodeCorrespondingToTree(Tree t) {
		return treeLookup.get(t);
	}

	/** @return The entry block of the control flow graph. */
	public SpecialBlock getEntryBlock() {
		return entryBlock;
	}

	/** @return The method this CFG corresponds to. */
	public MethodTree getTree() {
		return tree;
	}

}
