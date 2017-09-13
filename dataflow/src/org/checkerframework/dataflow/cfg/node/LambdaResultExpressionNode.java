package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.ExpressionTree;
import java.util.Collection;
import java.util.Collections;
import javax.lang.model.util.Types;
import org.checkerframework.dataflow.util.HashCodeUtils;
import org.checkerframework.javacutil.InternalUtils;

/*>>>
import org.checkerframework.checker.nullness.qual.Nullable;
*/

/**
 * A node for the single expression body of a single expression lambda
 *
 * @author Lazaro Clapp
 */
public class LambdaResultExpressionNode extends Node {

    protected ExpressionTree tree;
    protected /*@Nullable*/ Node result;

    public LambdaResultExpressionNode(
            ExpressionTree t, /*@Nullable*/ Node result, Types types /*,
            LambdaExpressionTree lambda,
            Symbol.MethodSymbol methodSymbol*/) {
        super(InternalUtils.typeOf(t));
        this.result = result;
        tree = t;
        //result.setAssignmentContext(new AssignmentContext.LambdaReturnContext(methodSymbol));
    }

    public Node getResult() {
        return result;
    }

    @Override
    public ExpressionTree getTree() {
        return tree;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitLambdaResultExpression(this, p);
    }

    @Override
    public String toString() {
        if (result != null) {
            return "-> " + result;
        }
        return "-> ()";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof LambdaResultExpressionNode)) {
            return false;
        }
        LambdaResultExpressionNode other = (LambdaResultExpressionNode) obj;
        if ((result == null) != (other.result == null)) {
            return false;
        }
        return (result == null || result.equals(other.result));
    }

    @Override
    public int hashCode() {
        return HashCodeUtils.hash(result);
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
