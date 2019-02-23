package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Types;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.node.AssignmentContext.LambdaReturnContext;
import org.checkerframework.dataflow.cfg.node.AssignmentContext.MethodReturnContext;

/**
 * A node for a return statement:
 *
 * <pre>
 *   return
 *   return <em>expression</em>
 * </pre>
 */
public class ReturnNode extends Node {

    protected final ReturnTree tree;
    protected final @Nullable Node result;

    public ReturnNode(ReturnTree t, @Nullable Node result, Types types, MethodTree methodTree) {
        super(types.getNoType(TypeKind.NONE));
        this.result = result;
        tree = t;
        result.setAssignmentContext(new MethodReturnContext(methodTree));
    }

    public ReturnNode(
            ReturnTree t,
            @Nullable Node result,
            Types types,
            LambdaExpressionTree lambda,
            MethodSymbol methodSymbol) {
        super(types.getNoType(TypeKind.NONE));
        this.result = result;
        tree = t;
        result.setAssignmentContext(new LambdaReturnContext(methodSymbol));
    }

    public Node getResult() {
        return result;
    }

    @Override
    public ReturnTree getTree() {
        return tree;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitReturn(this, p);
    }

    @Override
    public String toString() {
        if (result != null) {
            return "return " + result;
        }
        return "return";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ReturnNode)) {
            return false;
        }
        ReturnNode other = (ReturnNode) obj;
        if ((result == null) != (other.result == null)) {
            return false;
        }
        return (result == null || result.equals(other.result));
    }

    @Override
    public int hashCode() {
        return Objects.hash(result);
    }

    @Override
    public Collection<Node> getOperands() {
        if (result == null) {
            return Collections.emptyList();
        } else {
            return Collections.singletonList(result);
        }
    }
}
