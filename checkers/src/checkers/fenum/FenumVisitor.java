package checkers.fenum;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompilationUnitTree;

import checkers.basetype.BaseTypeVisitor;
import checkers.source.Result;
import checkers.types.AnnotatedTypeMirror;

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
    		checker.report(Result.failure("binary.type.incompatible", lhs, rhs), node);
    	}
    	
    	return super.visitBinary(node, p);
    }
}
