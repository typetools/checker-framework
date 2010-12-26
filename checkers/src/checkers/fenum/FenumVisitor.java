package checkers.fenum;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;

import checkers.basetype.BaseTypeVisitor;
import checkers.source.Result;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.util.TreeUtils;

public class FenumVisitor extends BaseTypeVisitor<Void, Void> {
    public FenumVisitor(FenumChecker checker, CompilationUnitTree root) {
        super(checker, root);
    }

    @Override
    public Void visitBinary(BinaryTree node, Void p) {
		if (!TreeUtils.isStringConcatenation(node)) {
			// TODO: ignore string concatenations
			
			AnnotatedTypeMirror lhs = atypeFactory.getAnnotatedType(node.getLeftOperand());
			AnnotatedTypeMirror rhs = atypeFactory.getAnnotatedType(node.getRightOperand());
			if (!(checker.getQualifierHierarchy().isSubtype(lhs.getAnnotations(), rhs.getAnnotations())
					|| checker.getQualifierHierarchy().isSubtype(rhs.getAnnotations(), lhs.getAnnotations()))) {
				checker.report(
						Result.failure("binary.type.incompatible", lhs, rhs),
						node);
			}
		}
        return super.visitBinary(node, p);
    }
    
    @Override
    protected boolean checkConstructorInvocation(AnnotatedDeclaredType dt,
            AnnotatedExecutableType constructor, Tree src) {
    	// Ignore the default annotation on the constructor
    	return true;
    }

    // TODO: should we require a match between switch expression and cases?
}
