package checkers.basetype;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;

/**
 * A visitor that checks the purity (as defined by {@link checkers.quals.Pure})
 * of a statement or expression.
 * 
 * @author Stefan Heule
 * 
 */
public class PurityChecker extends TreePathScanner<Boolean, Void> {
    
    public boolean isPure(TreePath methodBodyPath) {
        Boolean res = scan(methodBodyPath, null);
        return res != null && res;
    }

    @Override
    public Boolean reduce(Boolean r1, Boolean r2) {
        // null is treated as false
        return r1 != null && r2 != null && r1 && r2;
    }
    
    @Override
    public Boolean visitBlock(BlockTree node, Void p) {
        if (node.getStatements().isEmpty()) {
            return true;
        }
        return super.visitBlock(node, p);
    }
    
    @Override
    public Boolean visitEmptyStatement(EmptyStatementTree node, Void p) {
        return true;
    }

    @Override
    public Boolean visitLiteral(LiteralTree node, Void p) {
        return true;
    }

    @Override
    public Boolean visitReturn(ReturnTree node, Void p) {
        return super.visitReturn(node, p);
    }
    
    @Override
    public Boolean visitMethodInvocation(MethodInvocationTree node, Void p) {
        // TODO Auto-generated method stub
        return super.visitMethodInvocation(node, p);
    }

}
