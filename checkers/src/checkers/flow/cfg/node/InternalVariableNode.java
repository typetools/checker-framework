package checkers.flow.cfg.node;

import java.util.Collection;
import java.util.Collections;

import javax.lang.model.element.Element;

import checkers.flow.util.HashCodeUtils;

import com.sun.source.tree.Tree;

/**
 * A use of a variable introduced by the {@link CFGBuilder}.
 * Instead of an AST {@link Tree}, internal variable uses
 * refer directly to {@link VariableDeclarationNode}s.
 * 
 * @author Stefan Heule
 * @author Charlie Garrett
 * 
 */

public class InternalVariableNode extends Node {

    protected VariableDeclarationNode decl;

    public InternalVariableNode(VariableDeclarationNode decl) {
        assert decl != null;
        this.decl = decl;
        this.type = decl.getType();
    }

    public VariableDeclarationNode getDeclaration() {
        return decl;
    }

    public String getName() {
        return decl.getName();
    }

    @Override
    public/* @Nullable */Tree getTree() {
        return null;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitInternalVariable(this, p);
    }

    @Override
    public String toString() {
        if (lvalue) {
            return getName() + " (lval)";
        } else {
            return getName();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof InternalVariableNode)) {
            return false;
        }
        InternalVariableNode other = (InternalVariableNode) obj;
        return getDeclaration().equals(other.getDeclaration());
    }

    @Override
    public int hashCode() {
        return HashCodeUtils.hash(getName());
    }

    @Override
    public Collection<Node> getOperands() {
        return Collections.emptyList();
    }
}
