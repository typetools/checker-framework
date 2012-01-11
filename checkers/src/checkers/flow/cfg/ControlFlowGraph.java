package checkers.flow.cfg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import checkers.flow.cfg.block.Block;
import checkers.flow.cfg.block.Block.BlockType;
import checkers.flow.cfg.block.ConditionalBlock;
import checkers.flow.cfg.block.ExceptionBlock;
import checkers.flow.cfg.block.SingleSuccessorBlock;
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
	protected IdentityHashMap<Tree, Node> treeLookup;

	public ControlFlowGraph(SpecialBlock entryBlock, MethodTree tree,
			IdentityHashMap<Tree, Node> treeLookup) {
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

	/**
	 * @return The list of all basic block in this control flow graph.
	 */
	// TODO: remove if not needed
	public List<Block> getAllBlocks() {
		ArrayList<Block> r = new ArrayList<>();
		Set<Block> visited = new HashSet<>();
		Queue<Block> worklist = new LinkedList<>();
		Block cur = entryBlock;
		visited.add(entryBlock);
		r.add(entryBlock);

		// traverse the whole control flow graph
		while (true) {
			if (cur == null)
				break;

			Queue<Block> succs = new LinkedList<>();
			if (cur.getType() == BlockType.CONDITIONAL_BLOCK) {
				ConditionalBlock ccur = ((ConditionalBlock) cur);
				succs.add(ccur.getThenSuccessor());
				succs.add(ccur.getElseSuccessor());
			} else {
				assert cur instanceof SingleSuccessorBlock;
				Block b = ((SingleSuccessorBlock) cur).getSuccessor();
				if (b != null) {
					succs.add(b);
				}
			}

			if (cur.getType() == BlockType.EXCEPTION_BLOCK) {
				ExceptionBlock ecur = (ExceptionBlock) cur;
				succs.addAll(ecur.getExceptionalSuccessors().values());
			}

			for (Block b : succs) {
				if (!visited.contains(b)) {
					visited.add(b);
					r.add(b);
					worklist.add(b);
				}
			}

			cur = worklist.poll();
		}

		return r;
	}

}
