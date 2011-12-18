package checkers.flow.cfg;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import checkers.flow.cfg.node.Node;

/**
 * Generate a graph description in the DOT language of a control graph.
 * 
 * @author Stefan Heule
 * 
 */
public class CFGDOTVisualizer {

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

			if (cur instanceof ConditionalBasicBlock) {
				ConditionalBasicBlock ccur = ((ConditionalBasicBlock) cur);
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
				for (BasicBlock b : cur.getSuccessors()) {
					sb2.append("    " + cur.hashCode() + " -> " + b.hashCode());
					sb2.append(";\n");
					if (!visited.contains(b)) {
						visited.add(b);
						worklist.add(b);
					}
				}
			}

			for (Entry<Class<?>, BasicBlock> e : cur.getExceptionalSuccessors().entrySet()) {
				BasicBlock b = e.getValue();
				Class<?> cause = e.getKey();
				String exception = cause.getCanonicalName();
				if (exception.startsWith("java.lang.")) {
					exception = exception.replace("java.lang.", "");
				}
				
				sb2.append("    " + cur.hashCode() + " -> " + b.hashCode());
				sb2.append(" [label=\""+exception+"\"];\n");
				if (!visited.contains(b)) {
					visited.add(b);
					worklist.add(b);
				}
			}

			cur = worklist.poll();
		}

		// definition of all nodes including their labels
		for (BasicBlock v : visited) {
			sb1.append("    " + v.hashCode() + " [");
			if (v instanceof ConditionalBasicBlock) {
				sb1.append("shape=polygon sides=8 ");
			} else if (v instanceof SpecialBasicBlockImpl) {
				sb1.append("shape=oval ");
			}
			sb1.append("label=\""
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
	 * @param bb
	 *            Basic block to visualize.
	 * @return String representation.
	 */
	protected static String visualizeContent(BasicBlock bb) {
		StringBuilder sb = new StringBuilder();
		
		// loop over contents
		List<Node> contents = new LinkedList<>(bb.getContents());
		if (bb instanceof ConditionalBasicBlock) {
			contents.add(((ConditionalBasicBlock) bb).getCondition());
		}
		boolean notFirst = false;
		for (Node t : contents) {
			if (notFirst) {
				sb.append("\\n");
			}
			notFirst = true;
			sb.append(prepareString(visualizeNode(t)));
		}
		
		// handle case where no contents are present
		if (sb.length() == 0) {
			if (bb instanceof SpecialBasicBlockImpl) {
				SpecialBasicBlockImpl sbb = (SpecialBasicBlockImpl) bb;
				switch (sbb.getType()) {
				case ENTRY:
					return "<entry>";
				case EXIT:
					return "<exit>";
				case EXCEPTIONAL_EXIT:
					return "<exceptional-exit>";
				}
			} else {
				return "?? empty ??";
			}
		}
		
		return sb.toString();
	}

	protected static String visualizeNode(Node t) {
		return t.toString() + "   [ "+visualizeType(t)+" ]";
	}

	protected static String visualizeType(Node t) {
		String name = t.getClass().getSimpleName();
		return name.replace("Node", "");
	}
	
	protected static String prepareString(String s) {
		return s.replace("\"", "\\\"");
	}
}
