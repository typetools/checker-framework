package checkers.flow.cfg.node;

import java.util.Collection;
import java.util.Collections;

import javax.lang.model.type.TypeMirror;

import checkers.flow.util.HashCodeUtils;
import checkers.util.InternalUtils;

import com.sun.source.tree.VariableTree;

/**
 * A node for a local variable declaration:
 * 
 * <pre>
 *   <em>modifier</em> <em>type</em> <em>identifier</em>;
 * </pre>
 * 
 * Note: Does not have an initializer block, as that will be translated to a
 * separate {@link AssignmentNode}.
 * 
 * @author Stefan Heule
 * 
 */
public class VariableDeclarationNode extends Node {
    static long uid = 0;

    protected/* @Nullable */VariableTree tree;
    protected String name;

    // TODO: make modifier accessible

    public VariableDeclarationNode(VariableTree t) {
        tree = t;
        name = tree.getName().toString();
        type = InternalUtils.typeOf(tree);
    }

    /**
     * Constructor for internally generated declarations without AST
     * {@link Tree}s.
     */
    public VariableDeclarationNode(String prefix, TypeMirror type) {
        this.tree = null;
        this.name = uniqueName(prefix);
        this.type = type;
    }

    static String uniqueName(String prefix) {
        return prefix + "#var" + uid++;
    }

    public String getName() {
        return name;
    }

    @Override
    public/* @Nullable */VariableTree getTree() {
        return tree;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitVariableDeclaration(this, p);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof VariableDeclarationNode)) {
            return false;
        }
        VariableDeclarationNode other = (VariableDeclarationNode) obj;
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

}
