package checkers.flow.controlflowgraph;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import checkers.flow.controlflowgraph.node.Node;

/**
 * Generate a graph description in the DOT language of a control graph.
 * 
 * @author Stefan Heule
 * 
 */
public class ControlFlowGraphDOTVisualizer {

	/**
	 * Output a graph description in the DOT language, representing the control
	 * flow graph starting at <code>entry</code>.
	 * 
	 * @param entry
	 *            The entry node of the control flow graph to be represented.
	 * @return String representation of the graph in the DOT language.
	 */
	public static String visualize(BasicBlock entry) {
		StringBuilder sb1 = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();
		Set<BasicBlock> visited = new HashSet<BasicBlock>();
		Queue<BasicBlock> worklist = new LinkedList<BasicBlock>();
		BasicBlock cur = entry;
		visited.add(entry);

		// header
		sb1.append("digraph {\n");
		sb1.append("    node [shape=rectangle];\n\n");

		// traverse control flow graph and define all arrows
		while (true) {
			if (cur == null)
				break;

			Set<BasicBlock> succs;

			if (cur instanceof ConditionalBasicBlock) {
				ConditionalBasicBlock ccur = ((ConditionalBasicBlock) cur);
				succs = ccur.getExceptionalSuccessors();
				BasicBlock thenSuccessor = ccur.getThenSuccessor();
				sb2.append("    " + ccur.hashCode() + " -> "
						+ thenSuccessor.hashCode());
				sb2.append(" [label=\"then\"];\n");
				if (!visited.contains(thenSuccessor)) {
					visited.add(thenSuccessor);
					worklist.add(thenSuccessor);
				}
				BasicBlock elseSuccessor = ccur.getElseSuccessor();
				sb2.append("    " + ccur.hashCode() + " -> "
						+ elseSuccessor.hashCode());
				sb2.append(" [label=\"else\"];\n");
				if (!visited.contains(elseSuccessor)) {
					visited.add(elseSuccessor);
					worklist.add(elseSuccessor);
				}
			} else {
				succs = cur.getSuccessors();
			}

			for (BasicBlock b : succs) {
				sb2.append("    " + cur.hashCode() + " -> " + b.hashCode());
				sb2.append(";\n");
				if (!visited.contains(b)) {
					visited.add(b);
					worklist.add(b);
				}
			}

			cur = worklist.poll();
		}

		// definition of all nodes including their labels
		for (BasicBlock v : visited) {
			sb1.append("    " + v.hashCode() + " [label=\""
					+ visualizeContent(v) + "\"];\n");
		}

		sb1.append("\n");
		sb1.append(sb2);

		// footer
		sb1.append("}\n");

		return sb1.toString();
	}

	/**
	 * Produce a string representation of the contests of a basic block.
	 * 
	 * @param v
	 *            Basic block to visualize.
	 * @return String representation.
	 */
	protected static String visualizeContent(BasicBlock v) {
		if (v instanceof ConditionalBasicBlock) {
			return visualizeConditionalContent((ConditionalBasicBlock) v);
		}
		StringBuilder sb = new StringBuilder();
		boolean b = false;
		for (Node t : v.getContents()) {
			if (b) {
				sb.append("\\n");
			}
			b = true;
			sb.append(prepareString(t.toString()));
		}
		if (sb.length() == 0) {
			return "<empty>"; // the empty node
		}
		return sb.toString();
	}

	/**
	 * Produce a string representation of the contests of a conditional basic
	 * block.
	 * 
	 * @param v
	 *            Basic block to visualize.
	 * @return String representation.
	 */
	protected static String visualizeConditionalContent(ConditionalBasicBlock v) {
		return "if ("+prepareString(v.getCondition().toString())+")";
	}
	
	protected static String prepareString(String s) {
		return s.replace("\"", "\\\"");
	}
}
