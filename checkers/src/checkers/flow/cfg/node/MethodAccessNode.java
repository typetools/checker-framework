package checkers.flow.cfg.node;

import java.util.Collection;
import java.util.Collections;

import javax.lang.model.element.Element;

import checkers.flow.util.HashCodeUtils;
import checkers.util.InternalUtils;
import checkers.util.TreeUtils;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;

/**
 * A node for a method access, including a method accesses:
 * 
 * <pre>
 *   <em>expression</em> . <em>method</em> ()
 * </pre>
 * 
 * @author Stefan Heule
 * 
 */
public class MethodAccessNode extends Node {

    protected Tree tree;
    protected String method;
    protected Node receiver;

    // TODO: add method to get modifiers (static, access level, ..)

    public MethodAccessNode(Tree tree, Node receiver) {
        assert TreeUtils.isMethodAccess(tree);
        this.tree = tree;
        this.type = InternalUtils.typeOf(tree);
        this.receiver = receiver;
        this.method = TreeUtils.getMethodName(tree);
    }

    public Element getElement() {
        if (tree instanceof MemberSelectTree) {
            return TreeUtils.elementFromUse((MemberSelectTree) tree);
        }
        assert tree instanceof IdentifierTree;
        return TreeUtils.elementFromUse((IdentifierTree) tree);
    }

    public Node getReceiver() {
        return receiver;
    }

    public String getMethodName() {
        return method;
    }

    @Override
    public Tree getTree() {
        return tree;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitMethodAccess(this, p);
    }

    @Override
    public String toString() {
        return getReceiver() + "." + method;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof MethodAccessNode)) {
            return false;
        }
        MethodAccessNode other = (MethodAccessNode) obj;
        return getReceiver().equals(other.getReceiver())
                && getMethodName().equals(other.getMethodName());
    }

    @Override
    public int hashCode() {
        return HashCodeUtils.hash(getReceiver(), getMethodName());
    }

    @Override
    public Collection<Node> getOperands() {
        return Collections.singletonList(receiver);
    }
}
