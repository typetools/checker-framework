package org.checkerframework.dataflow.util;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import org.checkerframework.dataflow.cfg.node.ConditionalOrNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TypesUtils;

/** A utility class to operate on a given {@link Node}. */
public class NodeUtils {

    /**
     * @return true iff {@code node} corresponds to a boolean typed expression (either the primitive
     *     type {@code boolean}, or class type {@link java.lang.Boolean})
     */
    public static boolean isBooleanTypeNode(Node node) {

        if (node instanceof ConditionalOrNode) {
            return true;
        }

        // not all nodes have an associated tree, but those are all not of a
        // boolean type.
        Tree tree = node.getTree();
        if (tree == null) {
            return false;
        }

        Type type = ((JCTree) tree).type;
        if (TypesUtils.isBooleanType(type)) {
            return true;
        }

        return false;
    }

    /**
     * @return true iff {@code node} is a {@link FieldAccessNode} that is an access to an array's
     *     length
     */
    public static boolean isArrayLengthFieldAccess(Node node) {
        if (!(node instanceof FieldAccessNode)) {
            return false;
        }
        FieldAccessNode fieldAccess = (FieldAccessNode) node;
        return fieldAccess.getFieldName().equals("length")
                && fieldAccess.getReceiver().getType().getKind() == TypeKind.ARRAY;
    }

    /** Returns true iff {@code node} is an invocation of the given method. */
    public static boolean isMethodInvocation(
            Node node, ExecutableElement method, ProcessingEnvironment env) {
        if (!(node instanceof MethodInvocationNode)) {
            return false;
        }
        ExecutableElement invoked = ((MethodInvocationNode) node).getTarget().getMethod();
        return ElementUtils.isMethod(invoked, method, env);
    }
}
