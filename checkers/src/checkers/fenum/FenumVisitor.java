package checkers.fenum;

import java.util.List;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.VariableTree;

import checkers.basetype.BaseTypeVisitor;
import checkers.source.Result;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.util.TreeUtils;


public class FenumVisitor extends BaseTypeVisitor<Void, Void> {
	public FenumVisitor(FenumChecker checker, CompilationUnitTree root) {
        super(checker, root);
    }
		    
    @Override
    public Void visitBinary(BinaryTree node, Void p) {
        AnnotatedTypeMirror lhs = atypeFactory.getAnnotatedType(node.getLeftOperand());
        AnnotatedTypeMirror rhs = atypeFactory.getAnnotatedType(node.getRightOperand());
    	if (! (checker.getQualifierHierarchy().isSubtype(lhs.getAnnotations(), rhs.getAnnotations()) ||
    			checker.getQualifierHierarchy().isSubtype(rhs.getAnnotations(), lhs.getAnnotations()) ) ) {
    		checker.report(Result.failure("fenum.binary.type.incompatible", lhs, rhs), node);
    	}
    	
    	return super.visitBinary(node, p);
    }
    
    // Copy of supermethod, only change error key.
    @Override
    public Void visitVariable(VariableTree node, Void p) {
        validateTypeOf(node);
        // If there's no assignment in this variable declaration, skip it.
        if (node.getInitializer() == null)
            return super.visitVariable(node, p);

        commonAssignmentCheck(node, node.getInitializer(), "fenum.assignment.type.incompatible", p);
        return null;
        
        // we copied the code from super, don't call it or the wrong error message is created
        // return super.visitVariable(node, p);
    }

    // Copy of supermethod, only change error key.
    @Override
    public Void visitAssignment(AssignmentTree node, Void p) {
        commonAssignmentCheck(node.getVariable(), node.getExpression(),
                "fenum.assignment.type.incompatible", p);
        return null;
        
        // we copied the code from super, don't call it or the wrong error message is created
        // return super.visitAssignment(node, p);
    }
    
    // Copy of supermethod, only change error key.
    @Override
    protected void checkArguments(List<? extends AnnotatedTypeMirror> requiredArgs,
            List<? extends ExpressionTree> passedArgs, Void p) {
        assert requiredArgs.size() == passedArgs.size();
        for (int i = 0; i < requiredArgs.size(); ++i)
            commonAssignmentCheck(requiredArgs.get(i),
                    passedArgs.get(i),
                    "fenum.argument.type.incompatible", p);
    }
    
    // Copy of supermethod, only change error key.
    @Override
    protected boolean checkMethodInvocability(AnnotatedExecutableType method,
            MethodInvocationTree node) {
        AnnotatedTypeMirror methodReceiver = method.getReceiverType().getErased();
        AnnotatedTypeMirror treeReceiver = methodReceiver.getCopy(false);
        treeReceiver.addAnnotations(atypeFactory.getReceiver(node).getAnnotations());

        if (!checker.isSubtype(treeReceiver, methodReceiver)) {
            checker.report(Result.failure("fenum.method.invocation.invalid",
                TreeUtils.elementFromUse(node),
                treeReceiver.toString(), methodReceiver.toString()), node);
            return false;
        }
        return true;
    }
}
