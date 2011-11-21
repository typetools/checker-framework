package checkers.flow.controlflowgraph.node;

import checkers.util.TypesUtils;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;

/**
 * A utility class to operate on a given {@link Node}.
 * 
 * @author Stefan Heule
 * 
 */
public class NodeUtils {

	/**
	 * @return true iff <code>node</code> corresponds to a boolean typed
	 *         expression (either the primitive type <code>boolean</code>, or
	 *         class type {@link java.lang.Boolean})
	 */
	public static boolean isBooleanTypeNode(Node node) {
		Type type = ((JCTree) node.getTree()).type;

		if (node instanceof ConditionalOrNode) {
			return true;
		}

		if (TypesUtils.isBooleanType(type)) {
			return true;
		}

		return false;
	}
}
